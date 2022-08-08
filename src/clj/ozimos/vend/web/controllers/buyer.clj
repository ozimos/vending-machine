(ns ozimos.vend.web.controllers.buyer
  (:require
   [ring.util.http-response :as http-response]
   [clojure.string :as str]
   [next.jdbc :as jdbc]
   [ozimos.vend.web.routes.utils :as r.utils]))

(defn coins->sorted-map [coins]
  (->> coins
       frequencies
       (sort-by first >)
       (into {})))

(defn get-buy-change [sorted-coins-map amount]
  (let [denominations (keys sorted-coins-map)
        amounts (reductions (fn [a [k v]]
                              (let [total-denom (* k v)]
                                (if (> total-denom a)
                                  (rem a k)
                                  (- a total-denom))))
                            amount sorted-coins-map)
        coins (map #(int (/ %1 %2)) amounts denominations)]
    (mapcat (fn [coin [k v]]
              (take (min coin v) (repeat k)))
            coins
            sorted-coins-map)))

(defn subtract-coin-map [old-coin-map diff-coin-map]
  (into {} (map (fn [[k v]] [k (- v (get diff-coin-map k 0))])) old-coin-map))

(defn coin-map->coins [m]
  (mapcat (fn [[k v]] (repeat v k)) m))

(defn max-buy [coins cost amount]
  (let [total-available (reduce + 0 coins)]
    (loop [buy-amount amount]
      (let [expected-spend (* buy-amount cost)
            sorted-coins-map (coins->sorted-map coins)
            buy-change (get-buy-change sorted-coins-map expected-spend)
            total-spend (reduce + 0 buy-change)]
        (if (or (> expected-spend total-available)
                (not= total-spend expected-spend))
          (if (pos-int? (dec buy-amount))
            (recur (dec buy-amount))
            (throw (ex-info "Insufficient change for this purchase"
                            {:type :insufficient-change
                             :amount buy-amount
                             :change buy-change
                             :cost cost
                             :coins coins})))
          {:buy-amount buy-amount
           :sorted-coins-map sorted-coins-map
           :buy-change buy-change
           :total-spend total-spend})))))



(defn str->int-arr [s]
  (->> (str/split s #",")
       (remove str/blank?)
       (map #(Integer/parseInt %))))

(defn str->total [s]
  (->> s str->int-arr (reduce + 0)))

(defn deposit
  [{{{amount :deposit} :body} :parameters
    ident :identity
    :as req}]
  (let [query-fn (r.utils/route-data-key req :query-fn)

        {deposit :deposit}
        (query-fn :deposit! {:deposit amount :id (:id ident)})]
    (http-response/ok {:message "Deposit successful"
                       :deposit (str->total deposit)})))

(defn buy
  [{{{:keys [product_id amount]} :body} :parameters
    {buyer-id :id} :identity
    :as req}]
  (try
    (let [{:keys [query-fn db-conn]} (r.utils/route-data req)

          {:keys [deposit amount_available cost seller_id]}
          (query-fn :prepare-buy! {:product_id product_id :id buyer-id})

          coins (str->int-arr deposit)

          {:keys [buy-amount buy-change sorted-coins-map total-spend]}
          (max-buy coins cost (min amount amount_available))

          sorted-change-map (coins->sorted-map buy-change)

          coins-balance (coin-map->coins (subtract-coin-map sorted-coins-map sorted-change-map))

          seller-deposit (str/join "," buy-change)

          new-deposit (str/join "," coins-balance)

          new-balance (reduce + 0 coins-balance)

          product-name
          (jdbc/with-transaction [tx db-conn]
            (let [{updated-buyer-deposit :deposit}
                  (query-fn tx :reset-deposit! {:deposit new-deposit :id buyer-id})
                  {updated-seller-deposit :deposit}
                  (if updated-buyer-deposit
                    (query-fn tx :deposit! {:deposit seller-deposit :id seller_id})
                    (throw (ex-info "Buyer deposit update failed" {:deposit new-deposit :id buyer-id})))
                  {product-name :product_name}
                  (if updated-seller-deposit
                    (query-fn tx :purchase-product!
                              {:type :insufficient-change
                               :cost cost
                               :id product_id
                               :amount buy-amount
                               :old-amount-available amount_available})
                    (throw (ex-info "Seller deposit update failed" {:deposit seller-deposit :id seller_id})))]
              (if product-name product-name
                  (throw (ex-info "Product update failed" {})))))]

      (if product-name
        (http-response/ok {:message "Buy successful"
                           :total-spend total-spend
                           :product-name product-name
                           :buy-amount buy-amount
                           :change (vec coins-balance)
                           :deposit new-balance})

        (http-response/conflict {:message "Purchase not successful. Please try again"})))
    (catch clojure.lang.ExceptionInfo e
      (if (= :insufficient-change (:type (ex-data e)))
        (http-response/bad-request {:message "Insufficient change for this purchase"})
        (throw e)))))

(defn reset
  [{ident :identity :as req}]
  (let [query-fn (r.utils/route-data-key req :query-fn)
        {deposit :deposit} (query-fn :reset-deposit! {:id (:id ident) :deposit ""})]
    (http-response/ok {:message "Deposit reset successfully"
                       :deposit (str->total deposit)})))