package edu.brown.cs.student.main.server.handlers.stories;

import java.io.IOException;

import java.io.StringWriter;
import java.io.PrintWriter;

import org.bson.BsonDocument;
import org.bson.Document;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.squareup.moshi.JsonAdapter;
import com.squareup.moshi.JsonDataException;
import com.squareup.moshi.Moshi;

import edu.brown.cs.student.main.server.handlers.MongoDBHandler;
import edu.brown.cs.student.main.server.types.Story;
import spark.Request;
import spark.Response;

/**
 * Handler class for the redlining API endpoint.
 */
public class StoryPostHandler extends MongoDBHandler {

    public StoryPostHandler(MongoClient mongoClient) {
        super(mongoClient);
    }

    /**
     * 
     *
     * @param request  the request to handle
     * @param response use to modify properties of the response
     * @return
     * @throws Exception (this is a required part of the interface)
     */
    @Override
    public Object handle(Request request, Response response) throws Exception {
        try {

            String data;
            if (request.body().length() != 0) {
                data = request.body();
            } else {
                data = request.queryParams("data");
            }

            if (data == null) {
                return serialize(handlerFailureResponse("error_bad_request",
                        "data payload <data> must be supplied as query param OR content body (jsonified Story data)"));
            }
            Moshi moshi = new Moshi.Builder().build();
            JsonAdapter<Story> adapter = moshi.adapter(Story.class);
            Story story;
            try {
                story = adapter.fromJson(data);
            } catch (JsonDataException | IOException e) {
                return serialize(handlerFailureResponse("error_bad_request",
                        "data payload <data> could not be converted to Story format"));
            }
            if (story == null) {
                return serialize(handlerFailureResponse("error_bad_request",
                        "data payload <data> was null after json adaptation"));
            }
            try {
                MongoDatabase database = mongoClient.getDatabase("InterTwine");
                MongoCollection<Document> collection = database.getCollection("stories");
                BsonDocument bsonDocument = story.toBsonDocument();
                Document document = Document.parse(bsonDocument.toJson());
                collection.insertOne(document);
                return serialize(handlerSuccessResponse(document));
            } catch (Exception e) {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                e.printStackTrace(pw);
                String sStackTrace = sw.toString();
                return serialize(handlerFailureResponse("error_datasource",
                        "Given story could not be inserted into collection: " + sStackTrace));
            }
        } catch (Exception e) {
            // TODO delete this
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            e.printStackTrace(pw);
            String sStackTrace = sw.toString();
            return serialize(handlerFailureResponse("error_datasource",
                    "Given story could not be inserted into collection: " + sStackTrace));
        }
    }

}
