# Coding Project

Design Document for Replicated Concurrency Control and Recovery Project 
Variables: 
● An Outputter singleton object to place messages into a file. 
● Tick variable to count for time (initialized to 0). 
● Transaction objects get created with an ID and contain a state (ready, waiting, 
no­ready­site and aborted) and a boolean for readonly. They also contain a list of 
Instructions. Instructions are also objects containing whether it is read or write, an 
index, a value if necessary and a start time. 
● Site objects each contain an array of size 21 to store values for an index, initialized to 
10. (Index 0 won’t be used in the array of indexes since indexes are numbered starting 
at 1.) There is another boolean array of size 21 representing each index to keep track 
of which indexes can be read (in case of Site failure). Only the indexes that the Site 
can write to are written to, ie odd indexes are stored at Site with ID (index number  % 
10) + 1 as stated in the syllabus. Each Site object also has a state (ready or failed) and 
contains two lock tables. One is a write lock table array. The transaction holding the 
lock is stored at the index that it holds. The read lock table is a list of lists since more 
than one transaction can hold a read lock. Again, each list of transactions lives at the 
index of the outer list where it holds the lock. For example, transactions at the third 
item in the read lock table hold a read lock on index 3. Lastly, there is a linked hash 
set of waiting transactions that take over locks as they are freed.  
● A Transaction Manager singleton object receives the parsed commands and 
determines where to read and write values. Also, it has a list of all the transactions. 
And an array holding each site (again, 0­10 where 0 is not used). 
Algorithms: 
● Available Copies Algorithm (ready from one available site, but write to available sites) 
● Wait­die avoidance protocol with optimization suggested in syllabus: abort younger 
transaction immediately, if they conflict with older transaction’s lock. If older wait. 
● Multiversion read consistency algorithm for read­only transaction. So read­only 
transactions read the values of indexes that were committed at the time the 
transaction started.

Run it with:
java -jar dv.jar relative-path-to-script [verbose]
