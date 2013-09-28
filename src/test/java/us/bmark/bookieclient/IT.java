package us.bmark.bookieclient;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import retrofit.RestAdapter;
import retrofit.RetrofitError;

import java.io.InputStream;
import java.util.Date;
import java.util.Properties;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class IT {

    RestAdapter adapter;
    BookieService service;
    static final String username;
    static final String apikey;
    static final String PROPS_FILE = "/local-it.properties";
    static final String RESOURCES_PATH = "src/test/resources";

    static {
        Properties props = new Properties();
        InputStream ins = IT.class
                .getResourceAsStream(PROPS_FILE);

        try {
            props.load(ins);
        } catch (Exception e) {
            e.printStackTrace();
            // do nothing here, we'll double check and print
            // in the method checkProperties
        }

        username = props.getProperty("username");
        apikey = props.getProperty("apikey");

    }

    @BeforeClass
    public static void checkProperties() {
        assertThat("username should be set in " + RESOURCES_PATH + PROPS_FILE
                , username, is(notNullValue()));
        assertThat("apikey should be set in " + RESOURCES_PATH + PROPS_FILE
                , apikey, is(notNullValue()));
    }

    @Before
    public void setupService() {
        adapter = new RestAdapter.Builder()
                .setServer("https://bmark.us")
                .build();
        adapter.setLogLevel(RestAdapter.LogLevel.FULL);
        service = adapter.create(BookieService.class);
    }

    @Test
    public void getBookmarks() {
        int requestedCount = 13;
        int requestedPage = 2;

        try {
            BookmarkList results =
                    service.listBookmarks(requestedCount, requestedPage);

            assertThat(results.bmarks, is(not(empty())));
            assertThat(results.bmarks.size(), is(equalTo(results.count)));
            assertThat(results.count, is(equalTo(requestedCount)));
        } catch (RetrofitError e) {
            e.printStackTrace();
            System.out.println(e.getUrl());
            System.out.println(e.getResponse().getBody().toString());
            System.out.println(e.isNetworkError());
            fail();
        }
    }

    @Test
    public void getUserBookmarks() {

        int requestedCount = 5;
        int requestedPage = 1;


        try {
            BookmarkList results =
                    service.listUserBookmarks(username, apikey, requestedCount, requestedPage);

            assertThat(results.bmarks, is(not(empty())));
            assertThat(results.bmarks.size(), is(equalTo(results.count)));
            assertThat(results.count, is(equalTo(requestedCount)));
        } catch (RetrofitError e) {
            e.printStackTrace();
            System.out.println(e.getUrl());
            System.out.println(e.getResponse().getBody().toString());
            System.out.println(e.isNetworkError());
            fail();
        }
    }

    @Test
    public void makeBookmarkThenDeleteIt() {
        final long TIME = new Date().getTime();
        NewBookmark bmark = new NewBookmark();
        bmark.url="http://foo.example.com/testing/java-client-it-test/" +TIME;
        bmark.tags="testing-tag-1 testing-tag-2 testing-java-client-IT-test" + TIME;
        bmark.description="THIS BOOKMARK PLACED BY Java Client Integration Test " + TIME;
        bmark.inserted_by="JAVA-CLIENT-INT-TEST";

        int initialCount = service.listUserBookmarkTagged(username,apikey,"testing-tag-1", 99, 0).count;

        NewBookmarkResponse response = service.bookmark(username,apikey,bmark);
        String hash = response.bmark.hash_id;
        assertThat(hash,is(notNullValue()));

        int postCount = service.listUserBookmarkTagged(username,apikey,"testing-tag-1", 99, 0).count;
        assertThat(postCount,is(initialCount+1));
        String deleteRespMsg = service.delete(username,apikey,hash).message;
        assertThat(deleteRespMsg,is(equalTo("done")));
        int finalCount = service.listUserBookmarkTagged(username,apikey,"testing-tag-1", 99, 0).count;
        assertThat(finalCount,is(equalTo(initialCount)));
    }

}
