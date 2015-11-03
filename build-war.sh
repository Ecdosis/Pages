#!/bin/bash
if [ ! -d pages ]; then
  mkdir pages
  if [ $? -ne 0 ] ; then
    echo "couldn't create pages directory"
    exit
  fi
fi
if [ ! -d pages/WEB-INF ]; then
  mkdir pages/WEB-INF
  if [ $? -ne 0 ] ; then
    echo "couldn't create pages/WEB-INF directory"
    exit
  fi
fi
if [ ! -d pages/WEB-INF/lib ]; then
  mkdir pages/WEB-INF/lib
  if [ $? -ne 0 ] ; then
    echo "couldn't create pages/WEB-INF/lib directory"
    exit
  fi
fi
rm -f pages/WEB-INF/lib/*.jar
cp dist/Pages.jar pages/WEB-INF/lib/
cp lib/*.jar pages/WEB-INF/lib/
rm pages/WEB-INF/lib/servlet-api-3.0.jar
cp web.xml pages/WEB-INF/
jar cf pages.war -C pages WEB-INF -C pages static
echo "NB: you MUST copy the contents of tomcat-bin to \$tomcat_home/bin"
