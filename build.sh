[ -d build ] && rm -rf build
find . -name *.java > result.txt
sort result.txt > sources.txt
mkdir build
javac src/com/fixme/Router.java -sourcepath @sources.txt -d build
javac broker/src/com/broker/com.broker.Broker.java -sourcepath @sources.txt -d build
javac market/src/com/market/Market.java -sourcepath @sources.txt -d build
rm -rf result.txt
rm -rf sources.txt


