<?xml version='1.0' encoding='ISO-8859-1' ?>
<!DOCTYPE helpset
  PUBLIC "-//Sun Microsystems Inc.//DTD JavaHelp HelpSet Version 2.0//EN"
         "../dtd/helpset_2_0.dtd">

<helpset version="1.0">

  <!-- title -->
  <title>LightZone - Help</title>

  <!-- maps -->
  <maps>
     <homeID>top</homeID>
     <mapref location="Dutch/Map.jhm"/>
  </maps>

  <!-- views -->
  <view>
    <name>TOC</name>
    <label>Table Of Contents</label>
    <type>javax.help.TOCView</type>
    <data>Dutch/LightZoneTOC.xml</data>
  </view>

  <view>
    <name>Search</name>
    <label>Search</label>
    <type>javax.help.SearchView</type>
    <data engine="com.sun.java.help.search.DefaultSearchEngine">
      Dutch/JavaHelpSearch
    </data>
  </view>

  <presentation default="true" displayviewimages="false">
     <name>main window</name>
     <size width="700" height="400" />
     <location x="200" y="200" />
     <title>LightZone - Online Help</title>
     <toolbar>
        <helpaction>javax.help.BackAction</helpaction>
        <helpaction>javax.help.ForwardAction</helpaction>
        <helpaction>javax.help.SeparatorAction</helpaction>
        <helpaction>javax.help.HomeAction</helpaction>
        <helpaction>javax.help.ReloadAction</helpaction>
        <helpaction>javax.help.SeparatorAction</helpaction>
        <helpaction>javax.help.PrintAction</helpaction>
        <helpaction>javax.help.PrintSetupAction</helpaction>
     </toolbar>
  </presentation>
  <presentation>
     <name>main</name>
     <size width="400" height="400" />
     <location x="200" y="200" />
     <title>LightZone - Online Help</title>
  </presentation>
</helpset>
