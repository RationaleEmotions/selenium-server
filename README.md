# selenium-server
A modified Selenium Server Standalone that has the TRACE calls disabled by default.

In order to build this standalone, you would need Maven ( This is a Maven project )

To build a Selenium standalone that can be used as either Grid Hub (or) Grid Node (or) as the Selenium Standalone server, after cloning this repository just run 

`mvn clean package`

You should see an uber jar created in the `target` folder called `selenium-server-1.0-SNAPSHOT-jar-with-dependencies.jar`.
Uber jars are generally a jar of jars i.e., they are jars that contain all their dependent jars also inside them.
 
You should be able to use this standalone jar just as how you would use the Selenium Standalone jar that you 
download from www.seleniumhq.org for your standalone or grid needs.

For more information on this please refer to my blog post https://rationaleemotions.wordpress.com/2016/08/05/disabling-trace-in-selenium/

**This project currently makes use of Selenium 2.53.1**
