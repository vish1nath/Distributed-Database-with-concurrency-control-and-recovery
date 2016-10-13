# Coding Project

implemented a distributed database, complete with multiversion concurrency control, deadlock avoidance, replication, and
failure recovery.We have simulated this environment in java using data using data variables and implementing Transaction,Site Manager and Transaction Manager classes.

Data
The data consists of 20 distinct variables x1, ..., x20 (the numbers between 1 and 20 will be referred to as indexes below). There are 10 sites numbered 1 to 10. A copy is indicated by a dot. Thus, x6.2 is the copy of variable x6 at site 2. The odd indexed variables are at one site each (i.e. 1 + index number mod 10 ). For example, x3 and x13 are both at site 4.Even indexed variables are at all sites. Each variable xi is initialized to the value 10i. Each site has an independent lock table. If that site fails, the lock table is erased.

Algorithm
-  implemented the available copies approach to replication using two phase locking (using read and write locks) at each site and validation at commit time. 
- Avoided deadlocks using the wait-die protocol in which older transactions wiat for younger ones, but younger ones abort rather than wait for older ones.
- For read-only transaction implemented multi-version read consistency. So read-only transactions read the values of indexes that were committed at the time the transaction started
 
Run it with:
java -jar dv.jar relative-path-to-script [verbose]

The input for this are the script files located in scripts folder.You can find the detialed report about this project in DesignDocument.pdf
