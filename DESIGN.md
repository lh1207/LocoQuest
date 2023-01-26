# - Design Document for LocoQuest

1)  Introduction to our app

2) Storyboard (screen mockups): Invision, FluidUI, Powerpoint, paint, etc... will be fine.

3) 2-4 Functional Requirements in the format (fill in the square brackets with your own words):

    As a [User]
    I want [Feature]
    So that I can [Do something]
    Elaborate each of these with several examples in Given When Then format:
        Given [Prerequisite]
        When [Series of Steps]
        Then [Expected Result]
    Be very specific on Given When Thens, as they should describe test parameters. Describe a specific set of outputs for a given set of inputs.  In other words, "Given that shoe polish is normally $2.89, and there is a buy one get one free offer, When the user purchases two shoe polishes, Then the final cost will be $2.89",  is much better than, "Given that the user selects an item, and the item is on sale, then the item is sold at the sale price", because it references a specific item, with specific input and output price.
    Notes:
        The Given/When/Then syntax will form the basis of our unit tests.  Consider different cases: good data, bad data, multiple sets of data, etc.

4) Class Diagram

    Show data classes (DTOs), Activities, Fragments, MVVM, etc.
    I use ArgoUML to model classes in a diagram.  Other tools work as well.

5) Class Diagram Description

    One or two lines for each class to describe  use of interfaces, JME classes, Dalvik (Android) classes and resources, interfaces, etc.  Don't worry about putting more than a few words to each class; this does not need to be thorough.

6) A Product Backlog

        Product backlog items are stories.  Stories should be features, like "Take a picture and post on Facebook", not technical, like "Make the database"
        Each story should be a separate Project in GitHub.  The title of the project should be the name of the story.
            As long as you have these stories listed as Projects in GitHub, I can look at your projects to see your backlog.  No need to repeat it in the design document.
        There should be more stories than you will finish in one sprint.  The Product Owner can choose from this list, which stories to complete in each sprint.

7) A scrum or kanban board, using GitHub projects (preferred), Trello, Scrumy.com, or something similar, that contains:

        A milestone for Sprint #1, with tasks associated.
        Tasked stories for Sprint #1
            Sprint tasks that elaborate on the stories, with technical details.
            These should be technical tasks that are required to implement one of the features.
            You only need to task out stories for Sprint #1.  You can task out stories for Sprint #2 and #3 as we get closer to those sprints.
                The Product Owner/Scrum Master/DevOps person on your team should select stories to play for each sprint, from the list of available stories in the Product Backlog.
        As long as you have the tasks in GitHub Projects, under Projects, and associated with milestones, you do not need to repeat them in the design doc.  I'll look at the Project and Milestone view to see them.

8) Scrum Roles, and who will fill those roles

9) Communication tool you will use for your 8:00 Sunday group stand up (Zoom, Teams, etc.)  If you choose a different tool and/or different time, that's fine, just indicate it in the document.
