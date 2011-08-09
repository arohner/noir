(ns noir.test.core
  (:use [noir.core]
        [noir.util.test])
  (:use [clojure.test])
  (:require [noir.util.crypt :as crypt]
            [noir.server :as server]
            [noir.util.middleware :as middleware]
            [noir.session :as session]
            [noir.options :as options]
            [noir.response :as resp]
            [noir.cookies :as cookies]
            [noir.validation :as vali]))

(deftest hashing
         (let [pass (crypt/encrypt "password")]
           (is (crypt/compare "password" pass)))) 

(deftest session-get-default
         (with-noir
           (is (nil? (session/get :noir)))
           (is (= "noir" (session/get :noir "noir")))))

(deftest cookies-get-default
         (with-noir
           (is (nil? (cookies/get :noir)))
           (is (= "noir" (cookies/get :noir "noir")))))

(deftest options-get-default
         (with-noir
           (is (nil? (options/get :noir)))
           (is (= "noir" (options/get :noir "noir")))))

(deftest json-resp
         (with-noir
           (-> (resp/json {:noir "web"})
             (has-content-type (content-types :json))
             (has-body "{\"noir\":\"web\"}"))))

(deftest flash-lifetime
         (with-noir
           (session/flash-put! "noir")
           (is (= "noir" (session/flash-get)))
           (is (= nil (session/flash-get)))))

(defpage "/test" {:keys [nme]}
         (str "Hello " nme))

(deftest route-test
         (-> (send-request "/test" {"nme" "chris"})
           (has-status 200)
           (has-body "Hello chris")))

(defpage "/test.json" []
         (resp/json {:json "text"}))

(deftest route-dot-test
         (-> (send-request "/test.json")
           (has-status 200)
           (content-type-contains #"application/json")
           (has-body "{\"json\":\"text\"}")))

(defpage "/utf" []
         "ąčęė")

(defpage foo "/foo" []
  "named-route")

(deftest named-route-test
  (-> (send-request "/foo")
      (has-status 200)
      (has-body "named-route")))

(deftest url-for-test
  (is (= "/foo" (url-for foo))))

(defpage [:post "/post-route"] {:keys [nme]}
  (str "Post " nme))

(deftest route-post-test
  (-> (send-request [:post "/post-route"] {"nme" "chris"})
      (has-status 200)
      (has-body "Post chris")))

(defpage named-route-with-post [:post "/foo"] []
  "named-post")

(deftest named-route-post-test
  (-> (send-request [:post "/post-route"] {"nme" "chris"})
      (has-status 200)
      (has-body "Post chris")))

(defpage route-one-arg "/one-arg/:id" {id :id})

(deftest url-args
  (is (= "/one-arg/5" (url-for route-one-arg :id 5))))

(deftest url-for-throws
  (is (thrown? Exception (url-for route-one-arg))))

(deftest wrap-utf
  (with-middleware [middleware/wrap-utf-8]
    (-> (send-request "/utf")
      (has-content-type "text/html; charset=utf-8")
      (has-body "ąčęė"))))

(deftest valid-emails
  (are [email] (vali/is-email? email)
       "testword@domain.com"
       "test+word@domain.com"
       "test_word@domain.com"
       "test'word@domain.com"
       "test`word@domain.com"
       "test#word@domain.com"
       "test=word@domain.com"
       "test|word@domain.com"
       "testword@test.domain.com"
       "te$t@test.com"
       "t`e's.t&w%o#r{d@t.e.s.t"
       "x@x.xx"))

(deftest invalid-emails
  (are [email] (not (vali/is-email? email))
       ".test@domain.com"
       "-@-.com"
       "test"
       "test.@domain.com"
       "test@com"))