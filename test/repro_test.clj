(ns repro-test
  (:require
   [clojure.test :refer [deftest is]]
   [fluree.db.api :as fdb]))


(def ledger "events/log")
(def schema-tx [{:_id              :_collection
                 :_collection/name :event
                 :_collection/doc  "Athens semantic events."}
                {:_id               :_predicate
                 :_predicate/name   :event/id
                 :_predicate/doc    "A globally unique event id."
                 :_predicate/unique true
                 :_predicate/type   :string}])


(defn new-event [x]
  {:_id        :event
   :event/id   (str "uuid-" x)})

(deftest repro
  (let [conn (fdb/connect "http://localhost:8090")]
    @(fdb/new-ledger conn ledger)
    (fdb/wait-for-ledger-ready conn ledger)
    @(fdb/transact conn ledger schema-tx)
    (run! #(->> %
                new-event
                vector
                (fdb/transact conn ledger)
                deref)
          (range 4))
    (is (seq (-> conn
                 (fdb/db ledger)
                 (fdb/query {:select ["*"]
                             :from   "event"})
                 deref)))
    (fdb/close conn)))
