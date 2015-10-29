package net.headlezz.udacityproject1;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;

import net.headlezz.udacityproject1.tmdbapi.Movie;

/**
 * Main activity is mainly for handling fragment transactions
 */
public class MainActivity extends AppCompatActivity implements MovieNavigation, FragmentManager.OnBackStackChangedListener {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle(getString(R.string.app_name));

        if(savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragmentHolder, new MovieListFragment())
                    .commit();
        }

        // activity needs to listen to fragment changes to adjust the home as up arrow
        getSupportFragmentManager().addOnBackStackChangedListener(this);
        shouldDisplayHomeUp();
    }

    /**
     * Replaces the list with a details view showing the selected movie
     * @param movie Movie to show
     */
    public void showDetailsFragment(Movie movie) {
        MovieDetailsFragment frag = MovieDetailsFragment.newInstance(movie);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragmentHolder, frag)
                .addToBackStack(MovieDetailsFragment.TAG)
                .commit();
    }

    public void shouldDisplayHomeUp(){
        boolean hasBackstack = getSupportFragmentManager().getBackStackEntryCount()>0;
        getSupportActionBar().setDisplayHomeAsUpEnabled(hasBackstack);
    }

    @Override
    public boolean onSupportNavigateUp() {
        getSupportFragmentManager().popBackStack();
        return true;
    }

    @Override
    public void onBackStackChanged() {
        shouldDisplayHomeUp();
    }
}
