# How to install server environment to run Alvis Web Interface locally

1. Install Java JDK 8u25
http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html

2. Install Git 2.2.1
http://git-scm.com/download/mac

3. Install NetBeans 8.0.2

4. Open Terminal and check, if Git is installed by hitting “git”

5. Create local directory where Git repository will be cloned to.

6. Go to that repository in Terminal.

7. Clone repository through Terminal by hitting: “git clone https://[USERNAME]@bitbucket.org/rfs/alvis-web-interface.git”

8. Open NetBeans and open project “alvis-web-interface” from local directory. Ignore all errors…

9. Right-click “AlvisWebInterface” in Projects tab

10. Click “Resolve Missing Server…”

11. *Add server*…

12. Create directory for a server and install GlassFish 4.1

13. Finish by accepting default settings for Domain Location.

14. Select GlassFish 4.1 server


—

### WARNING! ACHTUNG! UWAGA! AVERTIZARE! WAARSKUWING!
This part should already be obsolete because of recent implementations in .properties files. For reference only…

—


15. If there is an error, go right-click “AlvisWebInterface” again and go to “Properties”

16. Set the “Web Pages Folder” directory to “alvis-Web-Interface/web” and the “WEB-INF Folder” directory to “alvis-Web-Interface/web/WEB-INF”

17. Go to “Services” tab next to the “Projects” tab, open “Servers” and right-click “GlassFish server 4.1” and select “Start”

18. Hit green play triangle in the top menu bar of NetBeans (Run Project). There may be some errors coming in the “Output” console below…

19. Hit the hammer and broom button (“Clean and Build Project”) next to the green triangle button.

20. If missing the source folder in “Project tab”, add the src folder in AlvisWebInterface Properties (through right-click).

21. Add libraries in Properties. Through “Add JAR/Folder” button add [alvis-web-interface/web/WEB-INF/lib] and the alvis.jar file (wherever it sits) and through “Add Library…” add Spring Framework 4.0.1., Spring Web MVC 4.0.1. and JSTL 1.2.2.

22. Hit hammer and broom button.

23. We were having this error: 
ant -f "/Users/marekkultys/Git Repos/alvis-web-interface" -Dnb.internal.action.name=rebuild -DforceRedeploy=false "-Dbrowser.context=/Users/marekkultys/Git Repos/alvis-web-interface" clean dist
/Users/marekkultys/Git Repos/alvis-web-interface/nbproject/build-impl.xml:237: Must set build.dir
BUILD FAILED (total time: 0 seconds)

If the error is “Must set build.dir”, go to build.xml file (ctrl+shift+o) and paste this code in the penultimate line just before the final </project> statement: 
	<property name="root.dir" value="./"/>
  <property name="build.dir" value="${root.dir}/build"/>

24. Refer to this article if there are problems:
http://www.adam-bien.com/roller/abien/entry/how_to_fix_the_libs

25. Update actual path of Java settings files. To do that, hit Command+comma in NetBeans and select Java tab, then Ant tab, and in Properties window add the actual path of absolute “org-netbeans-modules-java-j2seproject-copylibstask.jar” file. The command line added to Properties should look something like this: 
libs.CopyLibs.classpath=/Applications/NetBeans/NetBeans 8.0.2.app/Contents/Resources/NetBeans/java/ant/extra/org-netbeans-modules-java-j2seproject-copylibstask.jar

26. Create “images” directory in “alvis-web-interface/build/web”.

27. Add Alvis project to NetBeans (File > Open Project).

28. Update libraries for Alvis. Right-click to go to Alvis “Properties”, select “Libraries” and click “Add JAR/Folder” and select all. Libraries in red in the “Properties” window can be deleted.

29. Pull content from the repository.

30. Quit NetBeans and run it again.

31. Play the “Alvis Web Interface” project.

32. If there is an error, try adding “/SB” to the end of URL. The address should look something like this: 
http://localhost:8080/alvis-web-interface/SB

