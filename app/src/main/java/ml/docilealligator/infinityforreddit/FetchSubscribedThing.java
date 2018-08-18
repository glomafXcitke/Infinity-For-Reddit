package ml.docilealligator.infinityforreddit;

import android.content.Context;
import android.net.Uri;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;

import java.util.ArrayList;
import java.util.Map;

class FetchSubscribedThing {
    interface FetchSubscribedSubredditsListener {
        void onFetchSubscribedSubredditsSuccess(ArrayList<SubscribedSubredditData> subscribedSubredditData,
                                                ArrayList<SubscribedUserData> subscribedUserData,
                                                ArrayList<SubredditData> subredditData);
        void onFetchSubscribedSubredditsFail();
    }

    private Context context;
    private RequestQueue requestQueue;
    private FetchSubscribedSubredditsListener mFetchSubscribedSubredditsListener;
    private ArrayList<SubscribedSubredditData> mSubscribedSubredditData;
    private ArrayList<SubscribedUserData> mSubscribedUserData;
    private ArrayList<SubredditData> mSubredditData;

    private String mLastItem;

    FetchSubscribedThing(Context context, RequestQueue requestQueue,
                         ArrayList<SubscribedSubredditData> subscribedSubredditData,
                         ArrayList<SubscribedUserData> subscribedUserData,
                         ArrayList<SubredditData> subredditData) {
        this.context = context;
        this.requestQueue = requestQueue;
        mSubscribedSubredditData = subscribedSubredditData;
        mSubscribedUserData = subscribedUserData;
        mSubredditData = subredditData;
    }

    void fetchSubscribedSubreddits(FetchSubscribedSubredditsListener fetchUserInfoListener, final int refreshTime) {
        if(refreshTime < 0) {
            mFetchSubscribedSubredditsListener.onFetchSubscribedSubredditsFail();
            return;
        }

        Uri uri = Uri.parse(RedditUtils.OAUTH_API_BASE_URI + RedditUtils.SUBSCRIBED_SUBREDDITS)
                .buildUpon().appendQueryParameter(RedditUtils.AFTER_KEY, mLastItem)
                .appendQueryParameter(RedditUtils.RAW_JSON_KEY, RedditUtils.RAW_JSON_VALUE).build();

        mFetchSubscribedSubredditsListener = fetchUserInfoListener;
        StringRequest commentRequest = new StringRequest(Request.Method.GET, uri.toString(), new Response.Listener<String>() {
            @Override
            public void onResponse(String response) {
                new ParseSubscribedThing().parseSubscribedSubreddits(response, mSubscribedSubredditData,
                        mSubscribedUserData, mSubredditData,
                        new ParseSubscribedThing.ParseSubscribedSubredditsListener() {

                            @Override
                            public void onParseSubscribedSubredditsSuccess(ArrayList<SubscribedSubredditData> subscribedSubredditData,
                                                                           ArrayList<SubscribedUserData> subscribedUserData,
                                                                           ArrayList<SubredditData> subredditData,
                                                                           String lastItem) {
                                mSubscribedSubredditData = subscribedSubredditData;
                                mSubscribedUserData = subscribedUserData;
                                mLastItem = lastItem;
                                if(mLastItem.equals("null")) {
                                    mFetchSubscribedSubredditsListener.onFetchSubscribedSubredditsSuccess(mSubscribedSubredditData,
                                            mSubscribedUserData, mSubredditData);
                                } else {
                                    fetchSubscribedSubreddits(mFetchSubscribedSubredditsListener, refreshTime);
                                }
                            }

                            @Override
                            public void onParseSubscribedSubredditsFail() {

                            }
                        });
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if(error instanceof AuthFailureError) {
                    new AcquireAccessToken(context).refreshAccessToken(requestQueue, new AcquireAccessToken.AcquireAccessTokenListener() {
                        @Override
                        public void onAcquireAccessTokenSuccess() {
                            fetchSubscribedSubreddits(mFetchSubscribedSubredditsListener, refreshTime - 1);
                        }

                        @Override
                        public void onAcquireAccessTokenFail() {}
                    });
                } else {
                    mFetchSubscribedSubredditsListener.onFetchSubscribedSubredditsFail();
                }
            }
        }) {
            @Override
            public Map<String, String> getHeaders() {
                String accessToken = context.getSharedPreferences(SharedPreferencesUtils.AUTH_CODE_FILE_KEY, Context.MODE_PRIVATE).getString(SharedPreferencesUtils.ACCESS_TOKEN_KEY, "");
                return RedditUtils.getOAuthHeader(accessToken);
            }
        };
        commentRequest.setTag(FetchComment.class);
        requestQueue.add(commentRequest);
    }
}