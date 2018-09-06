import com.github.javafaker.*;

import org.apache.commons.lang3.ArrayUtils;
import org.neo4j.helpers.ArrayUtil;
import org.neo4j.kernel.configuration.*;
import org.neo4j.graphdb.factory.*;
import org.neo4j.graphdb.*;
import org.neo4j.graphdb.traversal.*;

import java.io.File;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.text.DateFormat;
import java.text.SimpleDateFormat;


import static org.neo4j.driver.v1.Values.parameters;

public class Main {
    private static String DB_PATH = "C:\\Users\\Qi\\.Neo4jDesktop\\neo4jDatabases\\database-664aa877-e854-480e-8e2c-65803aeb27d3\\installation-3.3.5\\data\\databases\\graph.db";

    private static enum RelTypes implements RelationshipType
    {
        CREATES,
        WAS_CREATED_BY,
        SUPERVISES,
        SUPERVISED_BY,
        HAS_COMMENT,
        COMMENTED_ON,
        WAS_COMMENTED_BY,
        BELONGS_TO,
    }

    public int DatabaseCreation() throws Exception
    {
        Faker generator = new Faker();
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        Boolean create_new_database = true;

        if (create_new_database == true) {
            // Delete old database
            try ( Transaction tx = graphDb.beginTx();
                  Result result = graphDb.execute("MATCH (a)-[b]->(c) DELETE b"))
            {
                tx.success();
            }
            try ( Transaction tx = graphDb.beginTx();
                  Result result = graphDb.execute("MATCH (a) DELETE a"))
            {
                tx.success();
            }

            // Data Creation
            try (Transaction tx = graphDb.beginTx()) {

                Label label_user = Label.label("User");
                Label label_card = Label.label("Card");
                Label label_comment = Label.label("Comment");

                int senior_manager_size = 2;
                int manager_size = 5;
                int employee_size = 23;
                int user_size = employee_size + manager_size + senior_manager_size;
                int card_size = 50;
                int comment_size = 100;
                int age_min = 20;
                int age_max = 50;
                int member_max = 3;
                Calendar cal = Calendar.getInstance();
                cal.set(Calendar.YEAR, 2018);
                cal.set(Calendar.MONTH, Calendar.JANUARY);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                Date date_start = cal.getTime();
                cal.set(Calendar.YEAR, 2019);
                cal.set(Calendar.MONTH, Calendar.JANUARY);
                cal.set(Calendar.DAY_OF_MONTH, 1);
                Date date_end = cal.getTime();

                Node[] nodes_user = new Node[user_size];
                Node[] nodes_card = new Node[card_size];
                Node[] nodes_comment = new Node[comment_size];
                Relationship relationship;

                String output = "";

                // Create some employees, managers, senior managers, and the relationships
                for (int i = 0; i < senior_manager_size; i++) {
                    nodes_user[i] = graphDb.createNode(label_user);
                    nodes_user[i].setProperty("username", generator.name().fullName());
                    nodes_user[i].setProperty("user_id", i);
                    nodes_user[i].setProperty("position", "Senior Manager");
                    nodes_user[i].setProperty("supervisor_id", -1);
                    nodes_user[i].setProperty("access", "admin");
                    nodes_user[i].setProperty("age", generator.number().numberBetween(age_min, age_max + 1));
                    output += nodes_user[i].getProperty("username").toString() + "," +
                              nodes_user[i].getProperty("user_id").toString() + "," +
                              nodes_user[i].getProperty("position").toString() + "," +
                              nodes_user[i].getProperty("supervisor_id").toString() + "," +
                              nodes_user[i].getProperty("access").toString() + "," +
                              nodes_user[i].getProperty("age").toString() + "\n";
                }
                for (int i = senior_manager_size; i < senior_manager_size + manager_size; i++) {
                    nodes_user[i] = graphDb.createNode(label_user);
                    nodes_user[i].setProperty("username", generator.name().fullName());
                    nodes_user[i].setProperty("user_id", i);
                    nodes_user[i].setProperty("position", "Manager");
                    nodes_user[i].setProperty("supervisor_id", generator.number().numberBetween(0, senior_manager_size));
                    nodes_user[i].setProperty("access", "admin");
                    nodes_user[i].setProperty("age", generator.number().numberBetween(age_min, age_max + 1));
                    output += nodes_user[i].getProperty("username").toString() + "," +
                              nodes_user[i].getProperty("user_id").toString() + "," +
                              nodes_user[i].getProperty("position").toString() + "," +
                              nodes_user[i].getProperty("supervisor_id").toString() + "," +
                              nodes_user[i].getProperty("access").toString() + "," +
                              nodes_user[i].getProperty("age").toString() + "\n";
                }
                for (int i = senior_manager_size + manager_size; i < user_size; i++) {
                    nodes_user[i] = graphDb.createNode(label_user);
                    nodes_user[i].setProperty("username", generator.name().fullName());
                    nodes_user[i].setProperty("user_id", i);
                    nodes_user[i].setProperty("position", "Employee");
                    nodes_user[i].setProperty("supervisor_id", generator.number().numberBetween(senior_manager_size, senior_manager_size + manager_size));
                    nodes_user[i].setProperty("access", "user");
                    nodes_user[i].setProperty("age", generator.number().numberBetween(age_min, age_max + 1));
                    output += nodes_user[i].getProperty("username").toString() + "," +
                              nodes_user[i].getProperty("user_id").toString() + "," +
                              nodes_user[i].getProperty("position").toString() + "," +
                              nodes_user[i].getProperty("supervisor_id").toString() + "," +
                              nodes_user[i].getProperty("access").toString() + "," +
                              nodes_user[i].getProperty("age").toString() + "\n";
                }
                Files.write(Paths.get("database_users.csv"), output.getBytes(), StandardOpenOption.CREATE);
                output = "";

                for (int i_senior_manager = 0; i_senior_manager < senior_manager_size; i_senior_manager++) {
                    for (int i_manager = senior_manager_size; i_manager < senior_manager_size + manager_size; i_manager++) {
                        String senior_manager_id = nodes_user[i_senior_manager].getProperty("user_id").toString();
                        String supervisor_id = nodes_user[i_manager].getProperty("supervisor_id").toString();

                        if (senior_manager_id.equals(supervisor_id)) {
                            relationship = nodes_user[i_senior_manager].createRelationshipTo(nodes_user[i_manager], RelTypes.SUPERVISES);
                            relationship = nodes_user[i_manager].createRelationshipTo(nodes_user[i_senior_manager], RelTypes.SUPERVISED_BY);
                            output += nodes_user[i_senior_manager].getId() + "," + RelTypes.SUPERVISES.toString() + "," + nodes_user[i_manager].getId() + "\n";
                            output += nodes_user[i_manager].getId() + "," + RelTypes.SUPERVISED_BY.toString() + "," + nodes_user[i_senior_manager].getId() + "\n";
                        }
                    }
                }
                for (int i_manager = senior_manager_size; i_manager < senior_manager_size + manager_size; i_manager++) {
                    for (int i_employee = senior_manager_size + manager_size; i_employee < user_size; i_employee++) {
                        String manager_id = nodes_user[i_manager].getProperty("user_id").toString();
                        String supervisor_id = nodes_user[i_employee].getProperty("supervisor_id").toString();

                        if (manager_id.equals(supervisor_id)) {
                            relationship = nodes_user[i_manager].createRelationshipTo(nodes_user[i_employee], RelTypes.SUPERVISES);
                            relationship = nodes_user[i_employee].createRelationshipTo(nodes_user[i_manager], RelTypes.SUPERVISED_BY);
                            output += nodes_user[i_manager].getId() + "," + RelTypes.SUPERVISES.toString() + "," + nodes_user[i_employee].getId() + "\n";
                            output += nodes_user[i_employee].getId() + "," + RelTypes.SUPERVISED_BY.toString() + "," + nodes_user[i_manager].getId() + "\n";
                        }
                    }
                }
                Files.write(Paths.get("database_users_rel.csv"), output.getBytes(), StandardOpenOption.CREATE);
                output = "";

                // Create some cards
                for (int i = 0; i < card_size; i++) {
                    nodes_card[i] = graphDb.createNode(label_card);
                    // Only senior manager and manager can create cards: "admin"
                    nodes_card[i].setProperty("owner_id", generator.number().numberBetween(0, senior_manager_size + manager_size));
                    nodes_card[i].setProperty("card_id", i);

                    Date date_create = generator.date().between(date_start, date_end);
                    nodes_card[i].setProperty("created_date", date_create.toString());
                    nodes_card[i].setProperty("due date", generator.date().between(date_create, date_end).toString());

                    Integer member_size = generator.number().numberBetween(1, member_max + 1);
                    int[] member_array = new int[member_size];
                    Arrays.fill(member_array, -1);
                    for (int i_member = 0; i_member < member_size; i_member++) {
                        int member_id = generator.number().numberBetween(0, user_size);
                        while (ArrayUtils.contains(member_array, member_id)) {
                            member_id = generator.number().numberBetween(0, user_size);
                        }
                        member_array[i_member] = member_id;
                    }
                    nodes_card[i].setProperty("members", member_array);

                    output += nodes_card[i].getProperty("owner_id").toString() + "," +
                              nodes_card[i].getProperty("card_id").toString() + "," +
                              nodes_card[i].getProperty("created_date").toString() + "," +
                              nodes_card[i].getProperty("due date").toString() + ",";
                    for (int i_member = 0; i_member < member_size; i_member++) {
                        output += Integer.toString(member_array[i_member]);
                        if (i_member != member_size - 1) {
                            output += ",";
                        }
                    }
                    output += "\n";
                }
                Files.write(Paths.get("database_cards.csv"), output.getBytes(), StandardOpenOption.CREATE);
                output = "";

                // Create some relationship between card and users
                for (int i_user = 0; i_user < user_size; i_user++) {
                    for (int i_card = 0; i_card < card_size; i_card++) {
                        String user_id = nodes_user[i_user].getProperty("user_id").toString();
                        String card_owner_id = nodes_card[i_card].getProperty("owner_id").toString();

                        if (user_id.equals(card_owner_id)) {
                            String date = nodes_card[i_card].getProperty("created_date").toString();
                            relationship = nodes_user[i_user].createRelationshipTo(nodes_card[i_card], RelTypes.CREATES);
                            relationship.setProperty("date", date);
                            relationship = nodes_card[i_card].createRelationshipTo(nodes_user[i_user], RelTypes.WAS_CREATED_BY);
                            relationship.setProperty("date", date);
                            output += nodes_user[i_user].getId() + "," + RelTypes.CREATES.toString() + "," + nodes_card[i_card].getId() + "\n";
                            output += nodes_card[i_card].getId() + "," + RelTypes.WAS_CREATED_BY.toString() + "," + nodes_user[i_user].getId() + "\n";
                        }
                    }
                }
                Files.write(Paths.get("database_cards_rel.csv"), output.getBytes(), StandardOpenOption.CREATE);
                output = "";


                // Create some comments
                for (int i = 0; i < comment_size; i++) {
                    nodes_comment[i] = graphDb.createNode(label_comment);
                    nodes_comment[i].setProperty("author_id", generator.number().numberBetween(0, user_size));
                    nodes_comment[i].setProperty("belong_to_card_id", generator.number().numberBetween(0, card_size));
                    nodes_comment[i].setProperty("content", generator.chuckNorris().fact());
                    output += nodes_comment[i].getProperty("author_id").toString() + "," +
                              nodes_comment[i].getProperty("belong_to_card_id").toString() + "," +
                              nodes_comment[i].getProperty("content").toString() + "\n";
                }
                Files.write(Paths.get("database_comments.csv"), output.getBytes(), StandardOpenOption.CREATE);
                output = "";

                // Create some relationship between comment and user
                for (int i_user = 0; i_user < user_size; i_user++) {
                    for (int i_card = 0; i_card < card_size; i_card++) {
                        for (int i_comment = 0; i_comment < comment_size; i_comment++) {
                            String user_id = nodes_user[i_user].getProperty("user_id").toString();
                            String card_id = nodes_card[i_card].getProperty("card_id").toString();
                            String author_id = nodes_comment[i_comment].getProperty("author_id").toString();
                            String belong_to_card_id = nodes_comment[i_comment].getProperty("belong_to_card_id").toString();

                            if (user_id.equals(author_id) && card_id.equals(belong_to_card_id)) {
                                // found a comment on card_id by user_id
                                DateFormat df = new SimpleDateFormat("EE MMM dd HH:mm:ss z yyyy", Locale.ENGLISH);
                                Date date_of_card = df.parse(nodes_card[i_card].getProperty("created_date").toString());

                                relationship = nodes_user[i_user].createRelationshipTo(nodes_comment[i_comment], RelTypes.COMMENTED_ON);
                                relationship.setProperty("date", generator.date().between(date_of_card, date_end).toString());

                                relationship = nodes_card[i_card].createRelationshipTo(nodes_comment[i_comment], RelTypes.HAS_COMMENT);
                                relationship.setProperty("date", generator.date().between(date_of_card, date_end).toString());

                                relationship = nodes_comment[i_comment].createRelationshipTo(nodes_user[i_user], RelTypes.WAS_COMMENTED_BY);
                                relationship.setProperty("date", generator.date().between(date_of_card, date_end).toString());

                                relationship = nodes_comment[i_comment].createRelationshipTo(nodes_card[i_card], RelTypes.BELONGS_TO);
                                relationship.setProperty("date", generator.date().between(date_of_card, date_end).toString());

                                output += nodes_user[i_user].getId() + "," + RelTypes.COMMENTED_ON.toString() + "," + nodes_comment[i_comment].getId() + "\n";
                                output += nodes_card[i_card].getId() + "," + RelTypes.HAS_COMMENT.toString() + "," + nodes_comment[i_comment].getId() + "\n";
                                output += nodes_comment[i_comment].getId() + "," + RelTypes.WAS_COMMENTED_BY.toString() + "," + nodes_user[i_user].getId() + "\n";
                                output += nodes_comment[i_comment].getId() + "," + RelTypes.BELONGS_TO.toString() + "," + nodes_card[i_card].getId() + "\n";
                            }
                        }
                    }
                }
                Files.write(Paths.get("database_comments_rel.csv"), output.getBytes(), StandardOpenOption.CREATE);
                output = "";

                System.out.println("Data was created");
                tx.success();
            }
        }
        graphDb.shutdown();

        return 0;
    }

    public int Queries() {
        GraphDatabaseService graphDb = new GraphDatabaseFactory().newEmbeddedDatabase(new File(DB_PATH));
        // Some changes has been made to database based on HW1 and teachers comments.
        // Removed unrelated relationships. Add more properties based on trello's model.
        // Now the structure of the database is:
        // Labels: User, Card, Comment
        //
        // Label User:
        // Properties:
        //     name
        //     position (Senior Manager, Manager, Employee)
        //     user_id
        //     access (admin, user) this is for the access of who can create the card
        //     ages
        //     supervisor_id
        //
        // Label Card:
        // Properties:
        //     card_id
        //     owner_id
        //     create_date
        //     due_date
        //     members
        //
        // Label Comment:
        // Properties:
        //     author_id
        //     belong_to_card_id
        //     content
        //
        // Relationships:
        //     (u:User u.access = "admin") -- [CREATES, date] --> (:Card),
        //     (:Card) -- [WAS_CREATED_BY, date] --> (u:User),
        //     (u:User u.position = "Senior Manager") --[SUPERVISES] --> (u:Users u.position = "Manager"),
        //     (u:User u.position = "Manager") --[SUPERVISES] --> (u:Users u.position = "Employee"),
        //     (u:User u.position = "Employee") --[SUPERVISED_BY] --> (u:Users u.position = "Manager"),
        //     (u:User u.position = "Manager") --[SUPERVISED_BY] --> (u:Users u.position = "Senior Manager"),
        //     (:Card) -- [HAS_COMMENT] --> (:Comment),
        //     (:Comment) -- [BELONGS_TO] --> (:Card),
        //     (:User) -- [COMMENTED_ON] --> (:Comment),
        //     (:Card) -- [WAS_COMMENTED_BY] --> (:User)
        //
        // The change makes the database's structure more close to the trello's.


        // Queries

        // Query 1 Get all managers information
        try (Transaction tx = graphDb.beginTx()) {
            Label label_user = Label.label("User");
            System.out.println("Query 1 Get all managers information");

            // get card with specified id
            ResourceIterator<Node> result = graphDb.findNodes(label_user, "position", "Manager");

            Node node_user = null;
            while (result.hasNext()) {
                node_user = result.next();
                System.out.printf("User: %-30s  Position: %-15s  ID: %-5s  Age: %-5s \n",
                                  node_user.getProperty("username").toString(),
                                  node_user.getProperty("position").toString(),
                                  node_user.getProperty("user_id").toString(),
                                  node_user.getProperty("age").toString());;
            }

            System.out.println("Query done.\n");
            tx.success();
        }

        // Query 2 Get all employess younger than 30
        try ( Transaction tx = graphDb.beginTx();
              Result result = graphDb.execute("MATCH (a:User) WHERE a.position = \"Employee\" AND a.age < 30 RETURN a"))
        {
            System.out.println("Query 2 Get all employess younger than 30");

            while (result.hasNext()) {
                Map<String,Object> row = result.next();
                for ( Map.Entry<String,Object> column : row.entrySet() )
                {
                    Node node_user = (Node)column.getValue();
                    System.out.printf("User: %-30s  Position: %-15s  ID: %-5s  Age: %-5s \n",
                                      node_user.getProperty("username").toString(),
                                      node_user.getProperty("position").toString(),
                                      node_user.getProperty("user_id").toString(),
                                      node_user.getProperty("age").toString());;


                }
            }
            System.out.println("Query done.\n");
            tx.success();
        }

        // Query 3 Will get all cards created by Senior Managers
        try (Transaction tx = graphDb.beginTx()) {
            Label label_card = Label.label("Card");
            Label label_user = Label.label("User");
            System.out.println("Query 3 Will get all cards created by Senior Managers");

            ResourceIterator<Node> result = graphDb.findNodes(label_card);

            Node node_card= null;
            while (result.hasNext()) {
                node_card = result.next();
                Integer owner_id = Integer.parseInt(node_card.getProperty("owner_id").toString());
                Node node_user = graphDb.findNode(label_user, "user_id", owner_id);
                if (node_user.getProperty("position").toString().equals("Senior Manager")) {
                    System.out.printf("Card ID:%s was created by %s who is a %s on: %s\n",
                            node_card.getProperty("card_id").toString(),
                            node_user.getProperty("username").toString(),
                            node_user.getProperty("position").toString(),
                            node_card.getProperty("created_date").toString());;
                }
            }

            System.out.println("Query done.\n");
            tx.success();
        }

        // Query 4 Find all the people related to a card with specific id
        // Data traversal API
        try (Transaction tx = graphDb.beginTx()) {
            Label label_card = Label.label("Card");
            Integer card_id = 5;
            System.out.println("Query 4 Show how many people are involved in one card issue");

            // get card with specified id
            ResourceIterator<Node> result = graphDb.findNodes(label_card, "card_id", card_id);

            Node card = null;
            while (result.hasNext()) {
                card = result.next();
                // traverse on relationships
                TraversalDescription td = graphDb.traversalDescription()
                        .breadthFirst()
                        .relationships(RelTypes.WAS_CREATED_BY, Direction.OUTGOING)
                        .relationships(RelTypes.HAS_COMMENT, Direction.OUTGOING)
                        .relationships(RelTypes.WAS_COMMENTED_BY, Direction.OUTGOING);

                Traverser traverser = td.traverse(card);

                // output result
                System.out.println("Card id: " + card_id);

                for (Path path : traverser) {
                    for (Relationship relationship : path.relationships()) {
                        Node node = relationship.getEndNode();
                        if (relationship.getType().name().equals(RelTypes.WAS_COMMENTED_BY.name())) {
                            Node node_user = relationship.getEndNode();
                            System.out.println("User " + node_user.getProperty("username").toString() +
                                               "(" + node_user.getProperty("position").toString() + ")" +
                                               " commented this card on " + relationship.getProperty("date"));
                        }
                        else if (relationship.getType().name().equals(RelTypes.WAS_CREATED_BY.name())) {
                            Node node_user = relationship.getEndNode();
                            System.out.println("User " + node_user.getProperty("username").toString() +
                                               "(" + node_user.getProperty("position").toString() + ")" +
                                               " created this card on " + relationship.getProperty("date"));
                        }
                    }

                }

                int []members =  (int [])card.getProperty("members");
                for (int i_member = 0; i_member < members.length; i_member++) {
                    Label label_user = Label.label("User");
                    Node node_user = graphDb.findNode(label_user, "user_id", members[i_member]);
                    System.out.println("User " + node_user.getProperty("username").toString() +
                            "(" + node_user.getProperty("position").toString() + ")" +
                            " is a member of this card.");
                }
            }

            System.out.println("Query done.\n");
            tx.success();
        }

        // Query 5 Organization
        // Data traversal API
        try (Transaction tx = graphDb.beginTx()) {
            Label label_user = Label.label("User");
            System.out.println("Query 5 Organization");

            // get user with specified id
            ResourceIterator<Node> result = graphDb.findNodes(label_user, "position", "Senior Manager");

            Node user = null;
            while (result.hasNext()) {
                user = result.next();
                // traverse on relationships "COMMENTED_ON"
                TraversalDescription td = graphDb.traversalDescription()
                        .depthFirst()
                        .relationships(RelTypes.SUPERVISES, Direction.OUTGOING);

                Traverser traverser = td.traverse(user);

                // output result
                for (Path path : traverser) {

                    for (Node node : path.nodes()) {
                        String name = node.getProperty("username").toString();
                        String position = node.getProperty("position").toString();
                        if (path.endNode() == node) {
                            System.out.println(position + ": " + name);
                        }
                        else {
                            System.out.print(position + ": " + name + "--[Supervises]-->");
                        }
                    }
                }
            }

            System.out.println("Query done.");
            tx.success();
        }

        graphDb.shutdown();

        return 0;
    }

    public static void main(String... args) throws Exception {
        Main database = new Main();
        //database.Queries();
        database.DatabaseCreation();
    }
}