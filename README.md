# LocoQuest

LocoQuest is an Android app that offers an interactive scavenger hunt experience using your device's geolocation capabilities. With LocoQuest, you can:

- Explore new places and take on a unique scavenger hunt by finding hidden treasures
- Utilize benchmarks provided by the National Geodetic Survey, which maintains a network of benchmarks across the United States
- Use benchmarks to provide a reference point for mapping and surveying
- Enjoy a fun and educational way to explore the world around you

Explore LocoQuest and start your adventure today!

# Storyboard

![image](https://user-images.githubusercontent.com/100445409/221301453-a6a2fe92-ed45-47a8-b9bc-bdd27a5b6e23.png)

# Functional Requirements
Given I've already authorized permissions for the app to use my device's geolocation capabilities within Android,
When I open LocoQuest from a clean-state and get sent to the "landing page," also known as the map,
Then geolocation shall be activated and display my current location on my device, so that I can find my current location and start my scavenger hunt.

Given there is no internet connectivity on my device,
When I open LocoQuest or the app refreshes,
Then the app shall display a warning message that alerts me that landmarks may not be up-to-date.

Given GPS is turned off on my device,
When I open LocoQuest at any point,
Then the map should turn darker, and the app should warn me that GPS is not enabled, which will throw off the calculations of the entire app despite the data stored locally on the device.

# UML Class Diagram

![image](https://user-images.githubusercontent.com/100445409/221331325-a4457e5a-6814-4841-8cae-2511a90700fb.png)

   - UserActivity, MainViewModel, MainActivity, ContextAwareViewModel, and BenchmarkInfoActivity are all related to the user interface of the app. They are responsible for displaying information to the user and receiving input from them.
   - RetrofitClientInstance is a class used to make HTTP requests to a server using the Retrofit library.
   - Benchmark.kt, LocationDetails.kt, User.kt, and Photo.kt are data transfer objects (DTOs) used to represent data that is being sent or received by the app.
   - IBenchmarkDAO and ILocalBenchmarkDAO are interfaces related to data access. They define the contract for accessing benchmark data from different sources.
   - BenchmarkDatabase is a class that represents a local database used by the app to store benchmark data. It is implemented using the Room library.
   - BenchmarkService.kt is a class that provides a service related to benchmarks. It interacts with a remote server to retrieve or update benchmark data.
   -RoomDatabase is a class provided by the Android framework that is used to create and manage local databases. It is used as the underlying implementation for BenchmarkDatabase.

# Product Backlog and Sprint Goals

Product Backlog
 - https://github.com/users/lh1207/projects/2

Milestones and Tasks for Sprint 1
  - Allow the user, when they open the app, to the see the map right away so that they can interact with it despite not being logged in
  - Implement JSON data of benchmarks from NGS to allow for Scavenging item and location parsing functionality
  - Implement Firebase to allow user to register/login and view profile and history
  - Design app through revisioned design document

Tasked Stories for Sprint 1
  - https://github.com/users/lh1207/projects/3/
  - https://github.com/users/lh1207/projects/8/
  - https://github.com/users/lh1207/projects/6/
  - https://github.com/users/lh1207/projects/9/

Scrum Roles, and who will fill those roles
  - Product Owner: Eric Miller
  - Scrum Master: Sam Dappen
  - Development Team: Levi Huff, Derrick Adkins, and William Bohman

# Weekly team meetings
The LocoQuest team gets together weekly at Sunday 6pm EST on Microsoft Teams
