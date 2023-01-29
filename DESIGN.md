# Design Document for LocoQuest

- Introduction
    - LocoQuest is an Android app developed in Kotlin that utilizes benchmarks provided by the National Geodetic Survey to offer an interactive scavenger hunt experience. With this app, you can take your adventure anywhere you desire and challenge yourself to find hidden treasures using your device's geolocation capabilities. The National Geodetic Survey (NGS) is an organization that maintains a network of benchmarks across the United States. These benchmarks are located on stable ground, such as on the foundation of a building or on a rock outcropping, and are used to measure the Earth's curvature and provide a reference point for mapping and surveying. The NGS has been collecting data and maintaining these benchmarks for over 200 years and now, with LocoQuest, you can use this network to explore new places and take upon a unique scavenger hunt.

- Storyboard (screen mockups):

    - ![image](https://github.com/lh1207/LocoQuest/blob/main/ScreenMockups/login_screen.png?raw=true) 
    - ![image](https://github.com/lh1207/LocoQuest/blob/main/ScreenMockups/map_screen.png?raw=true)
    - ![image](https://github.com/lh1207/LocoQuest/blob/main/ScreenMockups/history_screen.png?raw=true)


- 2-4 Functional Requirements in the format (fill in the square brackets with your own words):
    - 1). As an explorer on LocoQuest, given I've already authorized permissions for the app to use my devices geolocation capabilities within Android, I want geolocation to be activated and display my current location on my device when I open the app from a clean-state and get sent to the "landing page," also known as the map.
    - 2). As a scavenger of LocoQuest, given there is no connectivity on the device, the app will warn the user that there is not internet connection and that landmarks may not be up to date. This is because GPS can work without the internet, but the up-to-date JSON data won't be locally imported onto the user's device, which can give them out-of-date landmarks, when I open the app or the app refreshes as this is when JSON data from the device locally to the database is checked.
    - 3). As an explorer on LocoQuest, given GPS is turned off on the device, the map will turn darker, and the app will warn the user that GPS is not enabled, which will throw off the calculations of the entire app despite the data stored locally on the device. This should happen at any point GPS is turned off and the app is turned on no matter the circumstances. 


- Class Diagram

    - ![image](https://user-images.githubusercontent.com/100445409/215296810-21949c79-ca17-41f0-8f75-0233242b2453.png)

- Class Diagram Description

    -One or two lines for each class to describe  use of interfaces, JME classes, Dalvik (Android) classes and resources, interfaces, etc.  Don't worry about putting more than a few words to each class; this does not need to be thorough.
    
    - Map Activity
        - Handles user interactions of the map, including zooming and panning, and displaying markers for benchmarks/scavenging locations.

    - Benchmark
        - Child of MapActivity; represents bookmark location, which includes name, coordinates, and description.

    - ScavengingLocation
        - Child of MapActivity;  represents scavenging location includes information such as its coordinates, name, and a list of scavenging items that can be found there.

    - ScavengingItem
        - Child of ScavengingLocation; represents an item that can be found at a scavenging location, and includes information such as its name and description.

    - UserProfile
        - Child of MapActivity; represents the user's profile, and includes information such as their username, scavenging locations visited, and scavenging items found.
        
    - ScavengingHistory
        - Child of UserProfile; represents the user's scavenging history, and includes information such as the scavenging locations visited, and scavenging items found.
       
    - ScavengingLocationDatabase
        - Child of MapActivity; represents the scavenging location database, and includes functionality for adding, removing, and updating scavenging locations, as well as retrieving a list of all scavenging locations.

    - ScavengingItemDatabase
        - Child of MapActivity; represents the scavenging item database, and includes functionality for adding, removing, and updating scavenging items, as well as retrieving a list of all scavenging items.

    - Overall...
        - The "MapActivity" class has an association relationship with "Benchmark", "ScavengingLocation", "UserProfile", "ScavengingLocationDatabase" and "ScavengingItemDatabase" classes. The "ScavengingLocation" class has an association relationship with "ScavengingItem" class. The "UserProfile" class has an association relationship with "ScavengingHistory" class.


- A Product Backlog
        https://github.com/users/lh1207/projects/2/views/1

- A scrum or kanban board, using GitHub projects (preferred), Trello, Scrumy.com, or something similar, that contains: A milestone for Sprint #1, with tasks associated.
        - Milestones and Tasks for Sprint #1: https://github.com/users/lh1207/projects/1/views/1
        -Tasked stories for Sprint #1: https://github.com/lh1207/LocoQuest/projects?query=is%3Aopen 

- Scrum Roles, and who will fill those roles
    - Product Owner: Eric Miller 
    - Scrum Master: Sam Dappen
    - Development Team: Levi Huff, Derrick Adkins, and William Bohman

- Communication tool you will use for your 8:00 Sunday group stand up (Zoom, Teams, etc.)  If you choose a different tool and/or different time, that's fine, just indicate it in the document.
    - We will be using Teams and have decided to be flexible with everyone's time, for example some will meet on Saturday evening, and others are flexible with almost anytime that works for the group since they're all online this semester.
