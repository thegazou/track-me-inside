package com.example.airport;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * Created by Mateli on 16.01.2017.
 */

public class MyFragment extends Fragment{

    @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment1, container, false);
        //Instancier vos composants graphique ici (faîtes vos findViewById)
        return view; }

}
