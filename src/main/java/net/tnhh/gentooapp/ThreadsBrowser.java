package net.tnhh.gentooapp;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.text.Html;
import android.text.Spanned;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.Future;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.lang.Runnable;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;





public class ThreadsBrowser extends FragmentActivity {

    public static class DisplayableItemData {
        public Spanned titleLine;
        public Spanned comments;
        public Spanned metaData;
        public Bitmap img;
        public int postID;
        public String boardID;
        public DisplayableItemData() {
        }
    }
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    static MyAdapter mAdapter;

    static List<Fragment> fList = new ArrayList<Fragment>();

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    static ViewPager mPager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.threads_browser);

        Fragment newFragment = new PostListingFragment();
        Bundle args = new Bundle();
        args.putInt(PostListingFragment.ARG_SECTION_TYPE, PostListingFragment.ARG_SECTION_TYPE_BOARD);
        args.putString(PostListingFragment.ARG_BOARD_ID, "g");
        newFragment.setArguments(args);
        fList.add(newFragment);
        mAdapter = new MyAdapter(getSupportFragmentManager());

        ListView listView = (ListView) findViewById(R.id.listView);

        mPager = (ViewPager)findViewById(R.id.pager);
        mPager.setAdapter(mAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.threads_browser, menu);
        return true;
    }

    public static class MyAdapter extends FragmentPagerAdapter {
        public MyAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public int getCount() {
            return fList.size();
        }

        @Override
        public Fragment getItem(int position) {
            Fragment fragment = fList.get(position);
            return fragment;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            Fragment fragment = fList.get(position);
            return String.format("/%s/%s",
                    fragment.getArguments().getString(PostListingFragment.ARG_BOARD_ID),
                    fragment.getArguments().getInt(PostListingFragment.ARG_THREAD_ID)).toUpperCase();
        }


    }

    /**
     * A dummy fragment representing a section of the app, but that simply
     * displays dummy text.
     */
    public class PostListingFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */

        ArrayAdapter<DisplayableItemData> threadsAdapter;
        public static final String ARG_SECTION_TYPE = "section_type";
        public static final String ARG_BOARD_ID = "section_id";
        public static final String ARG_THREAD_ID = "thread_id";
        public static final int ARG_SECTION_TYPE_BOARD = 0;
        public static final int ARG_SECTION_TYPE_THREAD = 1;

        private int nextPage = 0;
        private Object jsonGroup = new Object();
        private Object imageGroup = new Object();

        private Future<JsonObject> loading;
        private int segType;
        private String boardID;
        private int threadID;

        private FragmentManager fm;

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            Log.d("Fragment", "Attaching fragment...");

            // create a tweet adapter for our list view
            threadsAdapter = new ArrayAdapter<DisplayableItemData>(activity, 0) {


                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    //Log.d("Fragment", String.format("Getting item %d...", position));
                    if (convertView == null) {
                        LayoutInflater inflater = (LayoutInflater) this.getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
                        convertView = inflater.inflate(R.layout.post_view2, null);
                    }

                    if ((segType == ARG_SECTION_TYPE_BOARD) && (nextPage != -1)) {
                        // we're near the end of the list adapter, so load more items
                        if (position >= getCount() - 3)
                            load4chanJSON(nextPage);
                    }

                    // grab the thread
                    DisplayableItemData currentItem = getItem(position);

                    ImageView imageView = (ImageView) convertView.findViewById(R.id.image);
                    imageView.setImageBitmap(currentItem.img);
                    TextView handle;

                    handle = (TextView)convertView.findViewById(R.id.metadata);

                    if (currentItem.titleLine != null)
                        handle.setText(currentItem.titleLine);

                    handle = (TextView)convertView.findViewById(R.id.content);
                    if (currentItem.comments != null)
                        handle.setText(currentItem.comments);

                    handle = (TextView)convertView.findViewById(R.id.smallmetadata);
                    if (currentItem.metaData != null)
                        handle.setText(currentItem.metaData);

                    return convertView;
                }
            };

        }

        public PostListingFragment() {


        }

        private void load4chanJSON(final int pgNum) {
            final FragmentActivity activity = this.getActivity();

            // don't attempt to load more if a load is already in progress
            if (loading != null && !loading.isDone() && !loading.isCancelled())
                return;

            String url = "";
            if (segType == ARG_SECTION_TYPE_BOARD) {
                url = String.format("https://api.4chan.org/%s/%d.json", boardID, pgNum);
            } else if (segType == ARG_SECTION_TYPE_THREAD) {
                url = String.format("https://api.4chan.org/%s/res/%d.json", boardID, threadID);
            }

            Log.d("BoardLoader", "Loading thread/board JSON...");

            nextPage = pgNum + 1;

            //Ion.getDefault(this.getActivity()).proxy("128.206.129.203", 8888);
            loading = Ion.with(activity, url)
                    //.setLogging("thread-fetcher", Log.DEBUG)
                    .group(jsonGroup)
                    .asJsonObject()
                    .setCallback(new FutureCallback<JsonObject>() {
                        @Override
                        public void onCompleted(Exception e, JsonObject result) {
                            Log.d("ThreadLoader", "Loading threads list done!");
                            //progressBar.setProgress(100);
                            // this is called back onto the ui thread, no Activity.runOnUiThread or Handler.post necessary.
                            if (e != null) {
                                //Toast.makeText(activity, "Error loading board or thread...", Toast.LENGTH_LONG).show();
                                Log.d("Fatal", e.toString());
                                return;
                            }
                            // add the tweets
                            JsonArray threads = result.get(
                                    segType == ARG_SECTION_TYPE_BOARD ? "threads" : "posts"
                            ).getAsJsonArray();

                            Log.d("ThreadLoader", "Adding to list... " + threads.size() + " items.");
                            if (threads.size() == 0)
                                nextPage = -1;

                            String imageUrl;
                            for (int i = 0; i < threads.size(); i++) {
                                DisplayableItemData cachedItem = new DisplayableItemData();

                                JsonObject currentItem = threads.get(i).getAsJsonObject();

                                /////////////////////////////////////////////////////////////////


                                String postTitle = null;
                                String imageUri = null;
                                String comments = null;
                                String smallMeta = null;

                                try {
                                    JsonObject post = null;

                                    if (segType == ARG_SECTION_TYPE_BOARD) {

                                        JsonArray posts = currentItem.get("posts").getAsJsonArray();
                                        post = posts.get(0).getAsJsonObject();

                                        cachedItem.postID = post.get("no").getAsInt();
                                        cachedItem.boardID = boardID;

                                        int repliesCount = post.get("replies").getAsInt();
                                        int imgCount = post.get("images").getAsInt();

                                        smallMeta =  String.format("<b>%d</b> replies, <b>%d</b> images. Click to view.", repliesCount, imgCount);

                                    } else if (segType == ARG_SECTION_TYPE_THREAD) {
                                        post = currentItem;
                                    } else {

                                    }

                                    if (post.get("sub") != null) {
                                        postTitle = String.format("%s <b>%s</b>", post.get("name").getAsString(), post.get("sub").getAsString());
                                    } else if (post.get("name") != null) {
                                        postTitle = String.format("%s", post.get("name").getAsString());
                                    } else {
                                        postTitle = "*Anonymous*";
                                    }


                                    if (post.get("tim") != null) {
                                        // set the profile photo using Ion
                                        imageUri = String.format(
                                                "https://0.thumbs.4chan.org/g/thumb/%ss%s",
                                                post.get("tim").getAsString(),
                                                ".jpg"/*post.get("ext").getAsString()*/);
                                        // "http://images.thumbs.4chan.org/g/thumb/" + post.get("filename").getAsString() + post.get("ext").getAsString();

                                        //Log.d("image-fetcher", imageUri);

                                        // start with the ImageView
                                        final DisplayableItemData finalCachedItem = cachedItem;
                                        Ion.with(getActivity(), imageUri)
                                                .asBitmap()
                                                .setCallback(new FutureCallback<Bitmap>() {
                                                    @Override
                                                    public void onCompleted(Exception e, Bitmap result) {
                                                        if (e != null) {
                                                            finalCachedItem.img = null;
                                                            return;
                                                        }
                                                        finalCachedItem.img = result;
                                                    }
                                                });
                                    } else {
                                        cachedItem.img = null;
                                    }

                                    comments = post.get("com").getAsString();

                                } catch (Exception err) {
                                    comments = null;
                                    postTitle = null;
                                }

                                if (postTitle != null)
                                    cachedItem.titleLine =  Html.fromHtml(postTitle);

                                if (comments != null)
                                    cachedItem.comments = Html.fromHtml(comments);

                                if (smallMeta != null)
                                    cachedItem.metaData = Html.fromHtml(smallMeta);


                                threadsAdapter.add(cachedItem);


                            }

                        }
                    });

        }

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            Log.d("Fragment", "Creating view...");

            boardID = getArguments().getString(ARG_BOARD_ID);
            segType = getArguments().getInt(ARG_SECTION_TYPE);
            fm = getSupportFragmentManager();

            if (segType == ARG_SECTION_TYPE_BOARD) {
                load4chanJSON(0);
            } else {
                threadID = getArguments().getInt(ARG_THREAD_ID);
                load4chanJSON(0);
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            Log.d("Fragment", "onCreateView()");
            if (container == null) {
                Log.i("Fragment", "onCreateView(): container = null");
            }

            View rootView = inflater.inflate(R.layout.tab_threads_browser, container, false);
            ListView listView = (ListView) rootView.findViewById(R.id.listView);

            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> arg0, View v, int position, long id) {
                    //Log.d("clicked", "Position:" + position);
                    if (arg0.getAdapter().getItem(position) instanceof DisplayableItemData) {
                        DisplayableItemData item = (DisplayableItemData) arg0.getAdapter().getItem(position);
                        if (item.postID != 0) {
                            // Create new fragment and transaction
                            Fragment newFragment = new PostListingFragment();
                            Bundle args = new Bundle();
                            args.putInt(PostListingFragment.ARG_SECTION_TYPE, PostListingFragment.ARG_SECTION_TYPE_THREAD);

                            args.putString(PostListingFragment.ARG_BOARD_ID, item.boardID);
                            args.putInt(PostListingFragment.ARG_THREAD_ID, item.postID);
                            newFragment.setArguments(args);
                            
                            fList.add(newFragment);
                            mAdapter.notifyDataSetChanged();
                            mPager.setCurrentItem(fList.size() - 1);
                        }
                    }
                }
            });

            listView.setAdapter(threadsAdapter);

            return rootView;
        }


    }


}
