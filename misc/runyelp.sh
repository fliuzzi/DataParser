rootdir="/home/qimeng/workspace/dataparser"

jars="${rootdir}/lib/cloud9-1.0.0.jar:${rootdir}/lib/lucene-queryparser-3.0.2.jar:${rootdir}/lib/commons-httpclient-3.1.jar:${rootdir}/lib/lucene-spatial-3.0.2.jar:${rootdir}/lib/commons-lang-2.6.jar:${rootdir}/lib/lucene-spellchecker-3.0.2.jar:${rootdir}/lib/commons-logging-1.1.1.jar:${rootdir}/lib/maxent-2.4.0.jar:${rootdir}/lib/hadoop-0.20.2-ant.jar:${rootdir}/lib/memcached-2.5-26-g0c7c5c1-a.jar:${rootdir}/lib/hadoop-0.20.2-core.jar:${rootdir}/lib/nlp.jar:${rootdir}/lib/hadoop-0.20.2-examples.jar:${rootdir}/lib/org.springframework.beans-3.0.3.RELEASE.jar:${rootdir}/lib/hadoop-0.20.2-test.jar:${rootdir}/lib/org.springframework.context-3.0.3.RELEASE.jar:${rootdir}/lib/hadoop-0.20.2-tools.jar:${rootdir}/lib/org.springframework.web-3.0.3.RELEASE.jar:${rootdir}/lib/javaee-api-5.0-1.jar:${rootdir}/lib/org.springframework.web.servlet-3.0.3.RELEASE.jar:${rootdir}/lib/JSON.jar:${rootdir}/lib/ostermillerutils_1_07_00.jar:${rootdir}/lib/json_simple.jar:${rootdir}/lib/search.jar:${rootdir}/lib/lucene-analyzers-3.0.2.jar:${rootdir}/lib/tika-core-0.8-SNAPSHOT.jar:${rootdir}/lib/lucene-collation-3.0.2.jar:${rootdir}/lib/tika-parsers-0.8-SNAPSHOT.jar:${rootdir}/lib/lucene-core-3.0.2.jar:${rootdir}/lib/trove-2.1.0.jar:${rootdir}/lib/lucene-misc-3.0.2.jar:${rootdir}/lib/where_utils.jar:${rootdir}/bin/dataparser.jar"



java -Xmx5g -Xms4g -cp ${jars} com.where.atlas.feed.yelp.Yelp80legs /home/qimeng/Yelp/raw_data/ /home/qimeng/test 30 true > log.txt &


