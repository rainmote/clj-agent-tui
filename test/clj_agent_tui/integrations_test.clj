(ns clj-agent-tui.integrations-test
  (:require
   [clojure.test :refer [deftest is]]
   [clj-agent-tui.integrations :as integrations]))

(deftest integration-seams-produce-stable-data
  (is (= {:type :mcp/server :name "local" :tools ["read"] :status :configured}
         (integrations/mcp-server "local" ["read"])))
  (is (= :lsp/request (:type (integrations/lsp-request-effect "textDocument/definition" {:uri "file.clj"}))))
  (is (= :subagent (get-in (integrations/subagent-effect "reviewer" "check diff") [:tool :source])))
  (is (= "integrations: mcp=1 lsp=configured subagents=1"
         (integrations/describe-integrations {:integrations {:mcp [{}]
                                                             :lsp {}
                                                             :subagents [{}]}}))))
