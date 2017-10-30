(ns nubank-project.core
  (:gen-class))

(def statement {:purchase - :deposit +})

(def accounts (ref (hash-map :XYZ (hash-map :balance (hash-map :171000 0 :171001 100 :171015 200 :171017 300 :171018 100 :171020 -600 :171025 400 :171030 0)
                                       :statement (hash-map :171001 [{:desc "purchase" :amount 200}]
                                                            :171012 [{:desc "purchase" :amount 300}
                                                                     {:desc "deposit" :amount 600}]
                                                            :171015 [{:desc "purchase" :amount 700}
                                                                     {:desc "deposit" :amount 800}]
                                                            :171017 [{:desc "purchase", :amount 200, :succeed "171020"},
                                                                      {:desc "deposit", :amount 100}]
                                                            :171018 [{:desc "purchase" :amount 200}
                                                                     {:desc "deposit" :amount 100}]
                                                            :171020 [{:desc "purchase" :amount 200}]
                                                            :171025 [{:desc "deposit" :amount 1000}]
                                                            :171030 [{:desc "purchase" :amount 400}]))
                        :ABC (hash-map :balance (hash-map :171000 0 :171018 -100 :171020 100)
                                       :statement (hash-map :171018 [{:desc "purchase" :amount 200}
                                                                     {:desc "deposit" :amount 100}]
                                                            :171020 [{:desc "deposit" :amount 200}]))
                        )))

(defmacro with-new-thread 
  "A function to execute code in another thread"
  [& body]
  `(.start (Thread. (fn [] ~@body))))

(defn now
  "Give you a date as a string in the format yyMMdd "
  [] (.format (java.text.SimpleDateFormat. "yyMMdd")(new java.util.Date)))

(defn zfill
  "Zero fill - takes a number and the length to pad to as arguments"
  [n c]
  (loop [s (str n)]
    (if (< (.length s) c)
      (recur (str "0" s))
      s)))

(defn get-first-key
  "A simple function to get the first key of a map"
  [m]
  (first (keys m)))

(defn keys-as-number
  "Convert a key to a number"
  [to-convert]
  (map #(Integer/parseInt %) (map name (keys to-convert))))

(defn num->key
  "Convert a number to a key"
  [num]
  (keyword(str num))
  )

(defn str->num
  "Convert a string to a number"
  [str]
  (Integer/parseInt (name str)))

(defn filter-by-val
  "This function take a predicat and a map to give you a pruned map"
  [pred m]
  (persistent!
    (reduce-kv (fn [acc k v]
                 (if (and (pred v) (not (zero? v))) ;exclude the 0
                   (conj! acc [k v])
                   acc))
               (transient {})
               m)))

(defn subtract-a-day
  "Subtract a day to a date in the format yyMMdd and update the month and year acordingly"
  [date]
  (let [list-date (re-seq #".{2}" date) year (str->num (first list-date)) month (str->num (second list-date)) day (str->num (last list-date))]
    (if (> 1 (dec day)) ; no gestion of 30 days month
      (if (> 1 (dec month))
        (str (zfill (dec year) 2) "12" "31") ; no differences is made between the 30 days and 31 days
        (str year (zfill (dec month) 2) "31"))
      (str year month (zfill (dec day) 2)
      )
    )
  ))


(defn add-a-day
  "Subtract a day to a date in the format yyMMdd and update the month and year acordingly"
  [date]
  (let [list-date (re-seq #".{2}" date) year (str->num (first list-date)) month (str->num (second list-date)) day (str->num (last list-date))]
    (if (< 31 (inc day)) ; no gestion of 30 days month
      (if (< 12 (inc month))
        (str (zfill (inc year) 2) "01" "01") ; no differences is made between the 30 days and 31 days and it can be         (str year (zfill (inc month) 2) "01"))
      (str year month (zfill (inc day) 2))))))

(defn closest-balance ; Step two
  "Get the most recent balance of an account"
  [date balance]
    (if (contains? balance (keyword date))
      (get balance (keyword date))
      (recur (subtract-a-day date) balance)))
;(dosync
;(closest-balance "171016" (get-in @accounts [:XYZ :balance]))
;(closest-balance "171030" (get-in @accounts [:XYZ :balance])))


(defn next-positive-balance
  "Give you a period of debt and the balance of the account in the position 0"
  ([date balance]
   (println date balance)
    (if (and (contains? balance (keyword date)) (< 0 (get balance (keyword date))))
      (println "inconsistent state")
      (next-positive-balance date balance (add-a-day date))))
  ([date-original balance new-date]
   (if (> (str->num new-date) (str->num(now)))
     {:balance (closest-balance date-original  balance)
      :start date-original
      :end "not yet"}
       (if (and (contains? balance (keyword new-date)) (< 0 (get balance (keyword new-date))))
         {:balance (closest-balance date-original  balance)
          :start date-original
          :end new-date}
         (recur date-original balance (add-a-day new-date))))))
;(dosync
;  (next-positive-balance "171020" (get-in @accounts [:XYZ :balance])))

(defn add-statement ; First Step
  "Add a statement and update the balance of the account"
  [account date desc amount accounts]
  (dosync
    (println account date desc amount)
    (let [operation (get statement (keyword desc))
          current-balance (closest-balance (now) (get-in @accounts [account :balance]))
          new-balance (operation current-balance amount)]
      (if (= date (now))
        (alter accounts update-in [account :statement (num->key date)] conj {:desc desc :amount amount})
        (alter accounts update-in [account :statement (num->key date)] conj {:desc desc :amount amount :succeed (now)})) ;(conj nil {}) give a () and not a []
      (alter accounts update-in [account :balance] conj {(keyword (now)) new-balance})
      )
    @accounts))
;(add-statement :XYZ (now) "purchase" 100 accounts)


(defn get-statement-and-balance ; Third step
  "Give all the statements for a period and the balance of the days accordingly"
  ([account accounts start]
     (get-statement-and-balance account accounts start (now)))
    ([account accounts start end]
     (dosync
       (let [ all-dates (filter #(and (<= % end) (<= start %)) (keys-as-number (get-in @accounts [account :statement])))
              statement-as-map (map #(hash-map (keyword (str %)) (get-in @accounts [account :statement (keyword (str %))])) all-dates)
              statements (map #(hash-map (get-first-key %) (into [(closest-balance (keyword (get-first-key %)) (get-in @accounts [account :balance]))] (get % (get-first-key %)))) statement-as-map)
             ]
         statements
     ))))
;(get-statement-and-balance :XYZ accounts 171020 171030)

(defn periods-of-debt ; Fourth step
  "Give all the period of debt with the balance of the period"
  [account balance]
  (let [negative-balance (filter-by-val neg? balance)]
    (println (keys negative-balance))
    (map #(next-positive-balance (name %) balance) (keys negative-balance))
    )
  )
;(dosync
;(periods-of-debt :XYZ (get-in @accounts [:XYZ :balance])))
