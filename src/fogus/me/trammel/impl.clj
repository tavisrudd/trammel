;;; impl.clj -- Contracts programming library for Clojure

;; by Michael Fogus - http://fogus.me/fun/
;; June 1, 2010

;; Copyright (c) Michael Fogus, 2010. All rights reserved.  The use
;; and distribution terms for this software are covered by the Eclipse
;; Public License 1.0 (http://opensource.org/licenses/eclipse-1.0.php)
;; which can be found in the file COPYING the root of this
;; distribution.  By using this software in any fashion, you are
;; agreeing to be bound by the terms of this license.  You must not
;; remove this notice, or any other, from this software.

(ns fogus.me.trammel.impl)

(defn build-constraints-map
  "Takes a list of the contract expectation bodies for each arity, of the form:

    (:requires (foo x) (bar x) :ensures (baz %))

   It then takes this form and builds a pre- and post-condition map of the form:

    {:pre  [(foo x) (bar x)]
     :post [(baz %)]}

   At the moment this function expects that the constraint functions are explicitly
   wrapped in a list with the argument(s) likewise explicit.
  "
  [expectations]
  (apply merge
         (for [[[dir] & [cnstr]] (->> expectations
                                      (partition-by #{:requires :ensures})
                                      (partition 2))] 
           {(case dir
                  :requires :pre
                  :ensures  :post)
            (vec cnstr)})))

(defn build-contract 
  "Expects a list representing an arity-based expectation of the form:

    (([x]) (:requires (foo x) :ensures (bar %)))

   This form is then destructured to pull out the arglist `[x]` and the
   contract expectation body (i.e. the constraints):

    (:requires (foo x) :ensures (bar %))

   It then uses this data to build another list reprsenting a specific arity body
   for a higher-order function with attached pre- and post-conditions that directly 
   calls the function passed in:

    ([f x] {:pre [(foo x)] :post [(bar %)]} (f x))
  "
  [[[sig] expectations :as c]]
  (list 
    (into '[f] sig)
    (build-constraints-map expectations)
    (list* 'f sig)))

(defn collect-bodies [forms]
  (for [body (->> (partition-by vector? forms)
                  (partition 2))]
    (build-contract body)))

(defn build-forms-map
  [forms]
  (for [[[e] & c] (map #(partition-by keyword? %) 
                       (if (vector? (first forms)) 
                         (list forms) 
                         forms))]
    {e (apply hash-map c)}))

