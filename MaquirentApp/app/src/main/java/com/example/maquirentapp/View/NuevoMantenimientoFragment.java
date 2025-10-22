package com.example.maquirentapp.View;

import androidx.lifecycle.ViewModelProvider;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.maquirentapp.R;
import com.example.maquirentapp.ViewModel.NuevoMantenimientoViewModel;

public class NuevoMantenimientoFragment extends Fragment {

    private NuevoMantenimientoViewModel mViewModel;

    public static NuevoMantenimientoFragment newInstance() {
        return new NuevoMantenimientoFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_nuevo_mantenimiento, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mViewModel = new ViewModelProvider(this).get(NuevoMantenimientoViewModel.class);
        // TODO: Use the ViewModel
    }

}