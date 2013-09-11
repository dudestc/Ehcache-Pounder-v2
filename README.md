Ehcache-Pounder-v2
==================

This is the official replacement for the old Ehcache-Standalone Pounder. The standalone pounder has been depreciated, as it contains legacy code, which is outdated and may cause issues with newer versions. 

The aim of the new V2 version, is to simplify the user management and configuration as well as add newer features to the pounder, such as the Search API. 


This is a small app that exercises the characteristics of the Enterprise
Ehcache tiered store, including BigMemory and the disk store.

Software and/or updates can be found here: 
https://github.com/sgrotz/Ehcache-Pounder-v2


1. DOWNLOAD ENTERPRISE EHCACHE

You must use a version of Enterprise Ehcache that supports BigMemory
to use this application.  You may download an evaluation copy of Enterprise
Ehcache and the license key from terracotta.org:

    http://terracotta.org/dl/dowload-bigmemory



2. RUN THE TUTORIAL

See the tutorial material on terracotta.org:
    http://terracotta.org/start/bigmemory-tutorial


3. BEFORE YOU START

Before you can use the pounder, make sure that you place the ehcache core libraries
in the pounder "libs" directory. 

<EhCachePounder>/libs
  - ehcache-core-ee.jar
  - ehcache-terracotta-ee-x.x.x.jar
  - terracotta-toolkit-x.x-runtime-ee-y.y.y.jar
  
The libraries (and their versions) are referenced from within the startup scripts, please make sure to amend them if needed. (It may be easier to just rename the files to match the  references in the startup scripts - that way, you won't need to change all of the startup scripts ;))


4. TEST DIFFERENT CONFIGURATIONS

Alter the configuration as set in the config.yml and ehcache.xml file. 


IMPORTANT: The offHeapSize in the config file + the heap size on the java
commandline + the memory needed by the OS MUST be less than the physical memory
on the machine.

NOTE: To alter the heap size, you must edit run-pounder.sh



CONFIGURATION PARAMETERS
threadCount
  The number of execution threads to run against the TSA.

entryCount
  The Total number of entries to load into the cache.

batchCount
  How often to print the current status and change the entry size for each thread.

maxValueSize
  Max size in bytes of the value portion of the entry (a random number is picked below this).

minValueSize
  Min size in bytes of the value portion of the entry (a random number is picked above this).

hotSetPercentage
  Percentage of time you hit an entry from the on-heap portion of the cache
  (see maxOnHeapCount).

rounds
  Number of times your execute entryCount operations matching the above config elements.

updatePercentage
  Number of times out of 100 that an entry is updated.

searchPercentage
  Number of times out of 100 that an entry is searched for.

ehcacheFileURL
  Specify where to find the ehcache.xml file. By default it should be in the root directory, use: ehcache.xml
  
ehcacheFileCacheName
  The ehcache file can contain several caches. Please specify here, which cache to use from the ehcache.xml file. For example: myTestCache
  


