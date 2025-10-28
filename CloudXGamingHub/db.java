import com.mongodb.client.*;
import org.bson.Document;

public class db {
    private static MongoCollection<Document> users;
    private static MongoCollection<Document> bookings;

    static {
        MongoClient client = MongoClients.create("mongodb+srv://ppraveen2150_db_user:3ugTieTt1dY5DjlY@cluster0.ikfdg4a.mongodb.net/?retryWrites=true&w=majority");
        MongoDatabase database = client.getDatabase("cloudxgaminghub");
        users = database.getCollection("users");
        bookings = database.getCollection("bookings");
    }

    public static boolean registerUser(String name, String pass) {
        if (users.find(new Document("name", name)).first() != null) return false;
        users.insertOne(new Document("name", name).append("pass", pass));
        return true;
    }

    public static boolean loginUser(String name, String pass) {
        Document doc = users.find(new Document("name", name).append("pass", pass)).first();
        return doc != null;
    }

    public static void bookSlot(String name, String game, String slot, int price) {
        bookings.insertOne(new Document("name", name)
                .append("game", game)
                .append("slot", slot)
                .append("price", price)
                .append("status", "booked"));
    }
}
