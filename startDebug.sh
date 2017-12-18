rsync -r --delete --exclude='*.class' /media/song/G680/songjh/projects/TGraph/source-code/demo-gephi-plugin/* ./

mvn -B -Dmaven.test.skip=true clean package

sed -i 's/Xms64m/Xms6g/' target/gephi-0.9.1/etc/gephi.conf
sed -i 's/Xmx512m/Xmx12g/' target/gephi-0.9.1/etc/gephi.conf
sed -i 's/EVERY_DAY/EVERY_DAY -J-Duser\.language=en/' target/gephi-0.9.1/etc/gephi.conf
# sed -i '/--jdkhome/a -Drun.params.debug="-J-Xdebug -J-Xnoagent -J-Xrunjdwp:transport=dt_socket,suspend=n,server=n,address=5005" \\' target/gephi-0.9.1/bin/gephi
if [ "$1" == "debug" ]
then
  sed -i '/--jdkhome/a  -J-Xdebug -J-Xrunjdwp:transport=dt_socket,server=y,suspend=n,address=5005 \\' target/gephi-0.9.1/bin/gephi
fi

mkdir -p target/userdir/config/Preferences/org/gephi/desktop/
echo 'check_latest_version=false' > target/userdir/config/Preferences/org/gephi/desktop/branding.properties
mkdir -p target/userdir/config/Preferences/org/netbeans/core/windows/tctracker/
echo 'TGraphDemoPanelTopComponent=true' >> target/userdir/config/Preferences/org/netbeans/core/windows/tctracker/overview.properties


mvn -B -Dmaven.test.skip=true org.gephi:gephi-maven-plugin:run
