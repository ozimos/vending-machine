(ns ozimos.vend.web.controllers.buyer-test
  "Unit Tests for buy controller functions"
(:require [clojure.test :refer :all]
          [ozimos.vend.web.controllers.buyer :as sut]))

(deftest coins-sorted-map-test
  (is (= {5 2 3 1 1 3} (sut/coins->sorted-map [1 5 3 1 1 5]))))

(deftest get-buy-change-test
  (is (= [100 100 100 50 50 50] (sut/get-buy-change {100 3, 50 3, 20 5, 10 4, 5 4}  450))))

(deftest max-buy-test
  (is (thrown-with-msg? clojure.lang.ExceptionInfo #"Insufficient change"
                        (sut/max-buy [10 100] 70 1)) "throws on insufficient change")

  (is (= {:buy-amount 1, :sorted-coins-map {100 1, 50 1, 20 1, 10 1}, :buy-change '(50 20), :total-spend 70}
         (sut/max-buy [10 20 50 100] 70 1)) "successful with change")

  (is (= {:buy-amount 1, :sorted-coins-map {50 1, 20 2, 10 1}, :buy-change '(50 20), :total-spend 70}
         (sut/max-buy [10 20 50 20] 70 2)) "reduces buy amount to match"))

(deftest subtract-coin-map-test
  (is (= {1 1 2 2} (sut/subtract-coin-map {1 3 2 6} {1 2 2 4}))))

(deftest coin-map->coins-test
  (is (= [1 1 1 2 2 2 2 2 2] (sut/coin-map->coins {1 3 2 6}))))