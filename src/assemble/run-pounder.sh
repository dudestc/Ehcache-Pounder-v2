java -server -verbose:gc -Xloggc:pounder.gc.log -XX:+PrintGCDetails -XX:+PrintTenuringDistribution -XX:+PrintGCTimeStamps -XX:+UseParallelGC -XX:MaxPermSize=512m -Dorg.terracotta.license.path=terracotta-license.key -Xms3g -Xmx3g -XX:+UseCompressedOops -XX:MaxDirectMemorySize=999G -cp "libs/*" org.sg.ehcache.pounder.EhcachePounderV2 
