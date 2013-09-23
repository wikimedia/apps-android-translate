
translatewiki.net Android app - 
-
Making translations in a MediaWiki site with the Translate extension, 
such as translatewiki.net or meta.wikimedia.org.
 
 * created by Or Sagi, 2013.

==
 
Introduction
============
This project is currently in development.
see more info at:	http://www.mediawiki.org/wiki/User:Orsagi/GSoC_2013_proposal
or project updates: http://www.mediawiki.org/wiki/User:Orsagi/Android_app_for_MediaWiki_translation/Project_Updates

Get this to work
================
The project is being developed using 'Android Studio', thus it might be optimal to build 
and run under the same.

dependencies for this project are:
---
* wikimedia/java-mwapi -
	https://github.com/wikimedia/java-mwapi.git
	
* ActionBarSherlock -
	http://actionbarsherlock.com/download.html
	
The easy way:
---
since apparently 'Android studio' is still not stable enough, a workaround for importing
dependencies would be:
1. create jar files  (using Eclipse for instance):
actionbarsherlock.jar
api.jar

2. put the jar files in translatewikiProject/translatewiki/libs/ 
3. on 'Android studio': right click on each jar file -> add as library.
4. make sure the 'dependencies' block in 'translatewiki/build.gradle' looks like:

		dependencies {
	    	compile 'com.android.support:support-v4:13.0.+'
	    	compile files('libs/api.jar', 'libs/actionbarsherlock.jar')
		}
	

Note:
 'com.android.support' is also needed even though it should be included as part of 'actionbarsherlock'.

=====
Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

	http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
