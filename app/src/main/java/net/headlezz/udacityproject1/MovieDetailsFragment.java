package net.headlezz.udacityproject1;

import android.app.ProgressDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import net.headlezz.udacityproject1.tmdbapi.Movie;
import net.headlezz.udacityproject1.tmdbapi.ReviewList;
import net.headlezz.udacityproject1.tmdbapi.TMDBApi;
import net.headlezz.udacityproject1.tmdbapi.Video;
import net.headlezz.udacityproject1.tmdbapi.VideoList;

import java.util.List;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit.Call;
import retrofit.Callback;
import retrofit.Response;
import retrofit.Retrofit;

/**
 * Fragment to show the details of a movie
 */
public class MovieDetailsFragment extends Fragment implements Callback<VideoList> {

    public static final String TAG = MovieDetailsFragment.class.getSimpleName();
    public static final String BUNDLE_ARG_MOVIE = "movie";

    @Bind(R.id.movie_details_ivPoster) ImageView ivPoster;
    @Bind(R.id.movie_details_tvTitle) TextView tvTitle;
    @Bind(R.id.movie_details_tvOverview) TextView tvOverview;
    @Bind(R.id.movie_details_tvReleaseDate) TextView tvReleaseDate;
    @Bind(R.id.movie_details_tvRating) TextView tvRating;
    @Bind(R.id.movie_details_trailerHolder) ViewGroup trailerHolder;

    Movie mMovie;
    VideoList mVideoList;

    /**
     * Hold a reference to the videolist download call so
     * it can be canceled when stopping
     */
    Call<VideoList> mVideoListCall;

    public static MovieDetailsFragment newInstance(Movie movie) {
        MovieDetailsFragment frag = new MovieDetailsFragment();
        Bundle args = new Bundle();
        args.putParcelable(BUNDLE_ARG_MOVIE, movie);
        frag.setArguments(args);
        return frag;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getArguments().containsKey(BUNDLE_ARG_MOVIE)) {
            mMovie = getArguments().getParcelable(BUNDLE_ARG_MOVIE);
            Log.d(TAG, "Bundled movie: " + mMovie.getTitle());
        } else
            throw new RuntimeException(TAG + " opened without bundled movie!");

        if(savedInstanceState != null && savedInstanceState.containsKey("videolist"))
            mVideoList = savedInstanceState.getParcelable("videolist");
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_movie_details, container, false);
        ButterKnife.bind(this, view);
        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TMDBApi.loadPoster(ivPoster, mMovie);
        tvTitle.setText(mMovie.getTitle());
        tvOverview.setText(mMovie.getOverview());
        String formatedDate = DateUtils.formatDateTime(tvReleaseDate.getContext(), mMovie.getReleaseDate().getTime(), DateUtils.FORMAT_SHOW_DATE | DateUtils.FORMAT_NUMERIC_DATE | DateUtils.FORMAT_SHOW_YEAR);
        tvReleaseDate.setText(getString(R.string.movie_release_date, formatedDate));
        tvRating.setText(getString(R.string.movie_rating, mMovie.getAvRating()));

        if(mVideoList == null)
            loadVideoList();
        else
            showVideoList(mVideoList);
    }

    private void loadVideoList() {
        if(mVideoListCall != null)
            mVideoListCall.cancel();
        mVideoListCall = TMDBApi.getVideosForMovie(mMovie, getString(R.string.api_key));
        mVideoListCall.enqueue(this);
    }

    @Override
    public void onResponse(Response<VideoList> response, Retrofit retrofit) {
        if(response.isSuccess()) {
            mVideoList = response.body();
            if(getActivity() != null)
                showVideoList(mVideoList);
        }
    }

    @Override
    public void onFailure(Throwable t) {
        if(getActivity() != null)
            Toast.makeText(getActivity(), "Failed to load videos: " + t.getMessage(), Toast.LENGTH_SHORT).show();
    }

    // TODO high res image for video link

    @OnClick(R.id.movie_details_btReviews)
    public void onReviewButtonClick(View view) {
        // TODO progressdialogs are bad because of rotation
        final ProgressDialog dialog = new ProgressDialog(getContext());
        dialog.setIndeterminate(true);
        dialog.show();
        TMDBApi.getReviewsForMovie(mMovie, getString(R.string.api_key)).enqueue(new Callback<ReviewList>() {
            @Override
            public void onResponse(Response<ReviewList> response, Retrofit retrofit) {
                dialog.dismiss();
                if(response.isSuccess()) {
                    ReviewDialogFragment frag = ReviewDialogFragment.newInstance(response.body());
                    frag.show(getChildFragmentManager(), ReviewDialogFragment.TAG);
                } else {
                    Log.e(TAG, "Query not successful.");
                }
            }

            @Override
            public void onFailure(Throwable t) {
                dialog.dismiss();
                Log.e(TAG, "something went wrong: " + t.getMessage());
            }
        });
    }

    /**
     * shows a list of links to youtube videos for the selected movie
     * if the videolist has no videos, the user sees a message that there are no.
     * @param videoList VideoList to show
     */
    private void showVideoList(@NonNull VideoList videoList) {
        List<Video> videos = videoList.getVideos();
        if(videos.size() > 0) {
            for (Video video : videoList.getVideos()) {
                View view = LayoutInflater.from(getActivity()).inflate(R.layout.movie_details_trailer_item, trailerHolder, false);
                TextView tv = (TextView) view.findViewById(R.id.trailer_item_tvName);
                tv.setText(video.getName());
                view.setTag(video);
                view.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        String videoId = ((Video) view.getTag()).getKey();
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoId));
                        intent.putExtra("VIDEO_ID", videoId);
                        try {
                            startActivity(intent);
                        } catch (ActivityNotFoundException e) {
                            Toast.makeText(getActivity(), "Youtube app not found.", Toast.LENGTH_SHORT).show();
                        }

                    }
                });
                trailerHolder.addView(view);
            }
        } else {
            trailerHolder.getChildAt(0).setVisibility(View.VISIBLE);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if(mVideoList != null)
            outState.putParcelable("videolist", mVideoList);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStop() {
        if(mVideoListCall != null)
            mVideoListCall.cancel();
        super.onStop();
    }
}
