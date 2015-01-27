(ns mu.core
  (:use seesaw.core)
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [seesaw.table :as table]))

(defprotocol VarsGateway
  (process-names [this])
  (vars-for-process [this process-name]))

(defprotocol ApplicationPresenter
  (show-process-names [this names])
  (show-process-vars [this vars])
  (start-up [this]))

(defprotocol ApplicationUseCases
  (get-vars-for [this process-name presenter])
  (get-process-names [this presenter]))

;; There was a gateway function here. Might return later

(defn application [db-gateway]
  (reify ApplicationUseCases
    (get-process-names [this presenter]
      (show-process-names presenter (process-names db-gateway)))
    
    (get-vars-for [this process-name presenter]
      (show-process-vars presenter (vars-for-process db-gateway process-name)))))

(defn presentation [use-case]
  (let [process-selector (combobox)
        row-columns [:columns  [{:key :var :text "Var"}
                                {:key :val :text "Val"}
                                {:key :cfg :text "Cfg"}]]
        vars-display (table :model row-columns)
        main-frame (frame :title "Mu"
                          :on-close :exit
                          :content (top-bottom-split process-selector
                                                     (scrollable vars-display)))
        presenter (reify ApplicationPresenter
                    (show-process-names [this names]
                      (config! process-selector :model (cons "" names)))
                    
                    (show-process-vars [this vars]
                      (config! vars-display
                               ;; The exception is probably happening here.
                               :model (table/table-model (concat row-columns [:rows vars]))))
                    
                    (start-up [this]
                      (-> main-frame
                          pack!
                          show!)))]
    (do
      (listen process-selector
              :selection (fn [_]
                           (get-vars-for use-case (selection process-selector) presenter)))
      (get-process-names use-case presenter)
      presenter)))

(def pgdb
  {:subprotocol "postgresql"
   :subname "//mri-staging/ppl"
   :user (System/getenv "USERNAME")
   :password (System/getenv "DB_PASS")})

;; (defn get-config [process_name]
;;   (if (not (string/blank? process_name))
;;     (jdbc/query pgdb
;;                 ["SELECT var, val, cfg FROM ops_automation.automated_process_vars WHERE process_name = ?;"
;;                  process_name])))

;; (defn get-processes []
;;   (cons "" (map :process_name
;;                 (jdbc/query pgdb "SELECT DISTINCT(process_name) from ops_automation.automated_process_vars;"))))

;; (defn process-selection [update-config-with-process]
;;   (let [view (combobox :model (get-processes))]
;;     (listen view :selection (fn [_] (update-config-with-process (selection view))))
;;     view))

;; (defn config-list [initial-config]
;;   (let [table-view (table
;;                     :model [:columns  [{:key :var :text "Var"}
;;                                        {:key :val :text "Val"}
;;                                        {:key :cfg :text "Cfg"}]
;;                             :rows initial-config])]
;;     [(scrollable table-view) table-view]))

;; (defn insert-if-defined [process_name]
;;   (fn [v]
;;     (when-let [config-rows (get-config process_name)]
;;       (apply (partial table/insert-at! v)
;;              (interleave (repeat 0)
;;                          config-rows)))))

;; (defn -main [& args]
;;   (invoke-later
;;    (let [[scrollable-config-view config-view] (config-list {})
;;          process-view (process-selection (fn [process_name] (-> config-view
;;                                                                table/clear!
;;                                                                ((insert-if-defined process_name)))))]
;;      (-> (frame :title "Mu"
;;                 :on-close :exit
;;                 :content (top-bottom-split process-view scrollable-config-view))
;;          pack!
;;          show!))))


(defn -main [& args]
  (let [database-gateway (gateway pgdb)
        use-cases (application database-gateway)
        rich-client (presentation use-cases)]
    (start-up rich-client)))
