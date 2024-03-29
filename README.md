<b>This project is considered obsolete and discontinued. For an example of integration with Excel that uses the most modern RTD mechanism, refer to this project: [Lightstreamer-example-StockList-client-rtd](https://github.com/Lightstreamer/Lightstreamer-example-StockList-client-rtd)</b>

# Lightstreamer - Basic Stock-List Demo - Excel (DDE) Client

<!-- START DESCRIPTION lightstreamer-example-stocklist-client-dde -->

This project includes a demo client showing integration between [Lightstreamer Java SE Client](http://www.lightstreamer.com/docs/client_javase_api/index.html) and [Dynamic Data Exchange (DDE)](http://en.wikipedia.org/wiki/Dynamic_Data_Exchange) Server for Excel.

## Live Demo

[![screenshot](screen_excel_large.png)](http://demos.lightstreamer.com/Java_DDEDemo_Basic/java-dde-stocklist-demo.zip)<br>
### [![](http://demos.lightstreamer.com/site/img/play.png) View live demo](http://demos.lightstreamer.com/Java_DDEDemo_Basic/java-dde-stocklist-demo.zip)<br>
(download `java-dde-stocklist-demo.zip`; unzip it; launch `start.bat`)<br>
*To run this demo, you must have Java installed in your machine. If you don't have Java already installed, please download it from [here] (http://www.oracle.com/technetwork/java/javase/downloads/index.html).<BR/>*

## Details

[Dynamic Data Exchange (DDE)](http://en.wikipedia.org/wiki/Dynamic_Data_Exchange) is a technology for communication between multiple applications under Microsoft Windows, often used for automatically filling a Microsoft Excel spreadsheet with data.<br>
This Java application is basically a DDE bridge, which injects the real-time updates received from Lightstreamer Server into an Excel spreadsheet. The quotes for 30 stock items are managed.<br>

The application connects to Lightstreamer Server on one side (by leveraging the <b>Java SE Client API for Lightstreamer</b>) and delivers the received data to any DDE-enabled application that is running on the PC on the other side by leveraging a third-party DDE component for Java ([JDDE by Pretty Tools](http://jdde.pretty-tools.com/)).<br>

Launch the application, then click on "Start Lightstreamer" to connect to Lightstreamer Server and subscribe to the quotes. You will see the update count spinning in the lower margin. Now click on "Copy Excel data to clipboard". The DDE subscription info will be copied into the clipboard. Open an Excel speadsheet and paste the data (CTRL-V). The real-time updates will show up on the spreadsheet.<br>

To temporarily stop the DDE Server, without closing the Lightstreamer connection, click on "Toggle data feeding to Excel".

### Dig the Code

The application is divided into 3 main public classes (alphabetical order).
* <b>LSDDEServer.java</b>: contains the actual DDE Server code. This part is responsible of receiving Lightstreamer data updates (and storing into an item cache) and feeding connected Excel instances trough postAdvise() update requests.
  For more information, please read the JDDE by Pretty Tools [API reference](http://jdde.pretty-tools.com/javadoc/index.html).
* <b>LightstreamerConnectionHandler.java</b>: a LSClient class wrapper, exporting handy methods (like connect, changeStatus, subscribe, etc.) used by the main <i>StockListDemo.java</i> class.
* <b>StockListDemo.java</b>: it's the main Java/Swing application that is controlling Lightstreamer client and DDE Server interfaces. It is composed by a JFrame and several JPanel. For more information, please read the Oracle JDK API reference.
  
Check out the sources for further explanations.
  
<i>NOTE: Not all the functionalities of the Lightstreamer Java SE Client & DDE Server demo are exposed by the classes listed above. You can easily expand those functionalities using the [Lightstreamer Java SE Client API](http://www.lightstreamer.com/docs/client_javase_api/index.html) as a reference.<br>
If in trouble, check out the [specific Lightstreamer forum](http://www.lightstreamer.com/vb/forumdisplay.php?f=12). </i>

<!-- END DESCRIPTION lightstreamer-example-stocklist-client-dde -->

## Install

If you want to install a version of this demo pointing to your local Lightstreamer Server, follow these steps:

* Note that, as prerequisite, the [Lightstreamer - Stock- List Demo - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-Stocklist-adapter-java) has to be deployed on your local Lightstreamer Server instance. Please check out that project and follow the installation instructions provided with it.
* Launch Lightstreamer Server.
* Download the `deploy.zip` file that you can find in the [deploy release](https://github.com/Lightstreamer/Lightstreamer-example-StockList-client-dde/releases) of this project and extract the `deployment_local` folder.
* Get the `ls-client.jar` file from [Lightstreamer distribution](http://www.lightstreamer.com/download/#ls5) (see compatibility notes below) in the `/DOCS-SDKs/sdk_client_java_se/lib` folder and put it in the `deployment_local/lib` folder.
The version required by this demo can be found in Lightstreamer version 5.1.2.
* Get the `pretty-tools-JDDE-2.0.3.jar` and `JavaDDEx64.dll` files from [pretty-tools-JDDE-2.0.3.zip](http://jdde.pretty-tools.com/downloads.php) and put the first in the `deployment_local/lib` folder and the latter in `deployment_local` folder.
  * Please note that if you run the demo on a 32-bit machine you have to choose `JavaDDE.dll` the intead of `JavaDDEx64.dll`.
* Launch `start.bat` from `deployment_local\` folder.

## Build

To build your own version of `java_dde_sld.jar`, instead of using the one provided in the deploy.zip file from the Install section above, and directly import the project as is you need the Eclipse IDE and
the JDDE by Pretty Tools Java library.<br>
For more information regarding the JDDE libraries that enables this application to communicate over DDE to Excel, go to [http://jdde.pretty-tools.com/](http://jdde.pretty-tools.com/).
For more information regarding Eclipse and how to run it, please go to [http://www.eclipse.org](http://www.eclipse.org), just download the latest version in its "classic" package.
  
<i>NOTE: You may also use the sources included in this project with another IDE or without any IDE but such approach is not covered in this readme. In any case, you always need the JDDE libraries.</i>

Obviously, you also need to have the Lightstreamer server installed somewhere. If you don't have it, go download it here: http://www.lightstreamer.com/download.htm and follow the instructions in the package to install it.
However, this release points to our demo Lightstreamer server, so if you just want to see how the application works, you can skip this step.
  
From your Lightstreamer installation, extract the files included in the `Lightstreamer/DOCS-SDKs/sdk_client_java_se/lib` folder and copy them into the `lib/` folder of this project.<br>
You're now ready to import the project into Eclipse.

From Eclipse, to compile and run the application, right-click on the project in the Package Explorer and click Run As -> Java Application.
At this point, your Lightstreamer Client & DDE Server demo is compiled and executed. Just click "Start Lightstreamer" to make the application connect to the configured Lightstreamer Push Server, then click on "Copy Excel data to clipboard", open a new Excel spreadsheet, right click on a cell and click "Paste". At this point, Excel will establish a hotlink to this demo application, which is simultaneously streaming real-time data to it.
Alternatively, you can use a launch script like this:
```cmd
@echo off

call "%JAVA_HOME%\bin\java.exe" -Dsystem.library.path=./ -cp "./pretty-tools_lib/pretty-tools-JDDE-2.0.3.jar";"./lib/ls-client.jar;./bin" javasedemo.dde.StockListDemo
pause
```
<br>
  
### Deploy
  
You may want to make the demo application point to your Lightstreamer server. As written above, this is not mandatory. To do so, open `src/javasedemo/dde/StockListDemo.java` and change the "PUSH_SERVER_URL" variable value.
The example requires that the [QUOTE_ADAPTER](https://github.com/Lightstreamer/Lightstreamer-example-Stocklist-adapter-java) has to be deployed in your local Lightstreamer server instance;
the [LiteralBasedProvider](https://github.com/Lightstreamer/Lightstreamer-example-ReusableMetadata-adapter-java) is also needed, but it is already provided by Lightstreamer server.<br>

## See Also

### Lightstreamer Adapters Needed by This Demo Client
<!-- START RELATED_ENTRIES -->

* [Lightstreamer - Stock-List Demo - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-Stocklist-adapter-java)
* [Lightstreamer - Reusable Metadata Adapters - Java Adapter](https://github.com/Lightstreamer/Lightstreamer-example-ReusableMetadata-adapter-java)

<!-- END RELATED_ENTRIES -->

### Related Projects

* [Lightstreamer - Stock-List Demos - HTML Clients](https://github.com/Lightstreamer/Lightstreamer-example-Stocklist-client-javascript)
* [Lightstreamer - Basic Stock-List Demo - jQuery (jqGrid) Client](https://github.com/Lightstreamer/Lightstreamer-example-StockList-client-jquery)
* [Lightstreamer - Stock-List Demo - Dojo Toolkit Client](https://github.com/Lightstreamer/Lightstreamer-example-StockList-client-dojo)
* [Lightstreamer - Basic Stock-List Demo - .NET Client](https://github.com/Lightstreamer/Lightstreamer-example-StockList-client-dotnet)
* [Lightstreamer - Basic Stock-List Demo - Java SE (Swing) Client](https://github.com/Lightstreamer/Lightstreamer-example-StockList-client-java)
* [Lightstreamer - Basic Stock-List Demo - Excel (RTD) Client](https://github.com/Lightstreamer/Lightstreamer-example-StockList-client-rtd)

## Lightstreamer Compatibility Notes

* Compatible with Lightstreamer Java Client API v. 2.5.2.
