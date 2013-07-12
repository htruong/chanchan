package net.tnhh.gentooapp;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import 	android.text.Html;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;
import com.koushikdutta.ion.ProgressCallback;

import java.util.Locale;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;


public class ThreadsBrowser extends FragmentActivity {

    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.threads_browser);
        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.threads_browser, menu);
        return true;
    }


    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
            Fragment fragment = new DummySectionFragment();
            Bundle args = new Bundle();
            args.putInt(DummySectionFragment.ARG_SECTION_NUMBER, position + 1);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 1;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.title_section1).toUpperCase(l);
                /*
                case 1:
                    return getString(R.string.title_section2).toUpperCase(l);
                case 2:
                    return getString(R.string.title_section3).toUpperCase(l);
                */
            }
            return null;
        }
    }

    /**
     * A dummy fragment representing a section of the app, but that simply
     * displays dummy text.
     */
    public static class DummySectionFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */

        ArrayAdapter<JsonObject> threadsAdapter;
        public static final String ARG_SECTION_NUMBER = "section_number";
        int nextPage = 0;
        Object jsonGroup = new Object();
        Object imageGroup = new Object();

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            Log.d("Fragment", "Attaching fragment...");

            // create a tweet adapter for our list view
            threadsAdapter = new ArrayAdapter<JsonObject>(activity, 0) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    Log.d("Fragment", String.format("Getting item %d...", position));
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                        convertView = inflater.inflate(R.layout.post_view2, null);
                    }

                    // we're near the end of the list adapter, so load more items
                    if (position >= getCount() - 3)
                        load(nextPage);

                    // grab the thread (or retweet)
                    JsonObject thread = getItem(position);


                    // grab the user info... name, profile picture, tweet text
                    JsonArray posts = thread.get("posts").getAsJsonArray();
                    JsonObject post = posts.get(0).getAsJsonObject();
                    String op = "";
                    String imageUrl = "";
                    String comments = "";
                    String smallmeta = "";


                    try {
                        if (post.get("sub") != null) {
                            op = String.format("%s <b>%s</b>", post.get("name").getAsString(), post.get("sub").getAsString());
                        } else if (post.get("name") != null) {
                            op = String.format("%s", post.get("name").getAsString());
                        } else {
                            op = "*Anonymous*";
                        }

                        // set the profile photo using Ion
                        imageUrl = String.format(
                                "https://0.thumbs.4chan.org/g/thumb/%ss%s",
                                post.get("tim").getAsString(),
                                ".jpg"/*post.get("ext").getAsString()*/);
                        // "http://images.thumbs.4chan.org/g/thumb/" + post.get("filename").getAsString() + post.get("ext").getAsString();
                        comments = post.get("com").getAsString();

                        int repliesCount = post.get("replies").getAsInt();
                        int imgCount = post.get("images").getAsInt();

                        smallmeta =  String.format("<b>%d</b> replies, <b>%d</b> images. Click to view.", repliesCount, imgCount);


                        Log.d("image-fetcher", imageUrl);

                        ImageView imageView = (ImageView) convertView.findViewById(R.id.image);

                        // Use Ion's builder set the google_image on an ImageView from a URL

                        // start with the ImageView
                        Ion.with(imageView)
                                // use a placeholder google_image if it needs to load from the network
                                //.placeholder(R.drawable.ic_launcher)
                                        // load the url
                                .load(imageUrl);
                    } catch (Exception e) {
                        comments = String.format("%s", e.getMessage());
                        op = "Error";
                    }

                    // and finally, set the name and text
                    TextView handle;

                    handle = (TextView)convertView.findViewById(R.id.metadata);
                    handle.setText(Html.fromHtml(op));

                    handle = (TextView)convertView.findViewById(R.id.content);
                    handle.setText(Html.fromHtml(comments));

                    handle = (TextView)convertView.findViewById(R.id.smallmetadata);
                    handle.setText(Html.fromHtml(smallmeta));

                    return convertView;
                }
            };

        }

        public DummySectionFragment() {


        }

        Future<JsonObject> loading;
        ProgressBar progressBar;

        private void load(int pgNum) {
            // don't attempt to load more if a load is already in progress
            if (loading != null && !loading.isDone() && !loading.isCancelled())
                return;


            // load the threads
            String url = String.format("https://api.4chan.org/g/%d.json", pgNum);
            /*
            if (threadsAdapter.getCount() > 0) {
                // load from the "last" id
                JsonObject last = tweetAdapter.getItem(tweetAdapter.getCount() - 1);
                url += "&max_id=" + last.get("id_str").getAsString();
            }
            */

            Log.d("ThreadLoader", "Loading threads list...");

            nextPage = pgNum + 1;
            progressBar.setVisibility(VISIBLE);
            progressBar.setProgress(1);

            //Ion.getDefault(this.getActivity()).proxy("128.206.129.203", 8888);
            loading = Ion.with(this.getActivity(), url)
                    .setLogging("thread-fetcher", Log.DEBUG)
                    .group(jsonGroup)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            Log.d("ThreadLoader", "Loading threads list done!");
                            progressBar.setProgress(100);
                            progressBar.setVisibility(GONE);
                            // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
                            if (e != null) {
                                Toast.makeText(null, "Error loading threads", Toast.LENGTH_LONG).show();
                                return;
                            }
                            // add the tweets
                            JsonArray threads = result.get("threads").getAsJsonArray();
                            Log.d("ThreadLoader", "Adding to list... " + threads.size() + " items.");
                            String imageUrl;
                            for (int i = 0; i < threads.size(); i++) {
                                threadsAdapter.add(threads.get(i).getAsJsonObject());
                                try {
                                    imageUrl = String.format(
                                            "https://0.thumbs.4chan.org/g/thumb/%ss%s",
                                            threads.get(i).getAsJsonObject().get("posts").getAsJsonArray().get(0).getAsJsonObject().get("tim").getAsString(),
                                            ".jpg"/*post.get("ext").getAsString()*/);
                                    Ion.with(getActivity(), imageUrl)
// for this image request, use a different group for images
                                            .group(imageGroup)
                                            .asBitmap();
                                } catch (Exception err) {

                                }
                            }

                        }
                    });
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {

            // basic setup of the ListView and adapter

            Log.d("Fragment", "Creating view...");

            View rootView = inflater.inflate(R.layout.tab_threads_browser, container, false);
            ListView listView = (ListView) rootView.findViewById(R.id.listView);

            progressBar = (ProgressBar) rootView.findViewById(R.id.progressBar);

            listView.setAdapter(threadsAdapter);

            load(0);

            //TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
            //dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            return rootView;
        }




    }

}
