# nubank-project

Explanation about the data structure

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

I choose to modelize each account by an asssociation key/value because that way with the id of the account you have an easy access to everything about a person, O(1).
An account is composed of two entries:
	- the balance, which is an historic of all the state of an account
	- the statements, which is an historic of all the operation that happened

I picked "yyMMdd" as format for the date because that way the date came as a number (ordered by time) and can easily manipulated (difference beetween two days, ordering days,... ).

The balance got an entry if it's state evolve, a day without statement won't appear here, that's in an effort to save memory and scalability.
The statement is a map with the date of the operation as key, that way it has a complexity O(1), and the value is a liste of map, each represent a statement, if the statement was postpone beside the description and the amount of the statement a third tuple succeed save the date of the enforcement. 

For the server part, my choice would have been a queue (RabbitMQ) where the "transaction" would go and workers at the end can proccess the tasks, that way si very scalable and enforce a good data flow.


