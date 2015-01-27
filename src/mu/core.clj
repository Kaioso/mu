(ns mu.core
  (:use seesaw.core))

(defn make-table []
  (scrollable (table
               :model [:columns  [{:key :var :text "Var"}
                                  {:key :val :text "Val"}
                                  {:key :cfg :text "Cfg"}]
                       :rows [{:var "one" :val "two" :cfg "any"}]])))

(defn split-main [upper lower]
  (top-bottom-split upper lower))

(defn -main [& args]
  (invoke-later
   (-> (frame :title "Mu"
              :on-close :exit
              :content (split-main (combobox) (make-table)))
       pack!
       show!)))
