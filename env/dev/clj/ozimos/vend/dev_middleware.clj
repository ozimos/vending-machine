(ns ozimos.vend.dev-middleware)

(defn wrap-dev [handler _opts]
  (-> handler))
