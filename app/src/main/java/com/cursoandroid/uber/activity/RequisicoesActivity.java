package com.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.cursoandroid.uber.adapter.RequisicoesAdapter;
import com.cursoandroid.uber.databinding.ActivityRequisicoesBinding;
import com.cursoandroid.uber.R;
import com.cursoandroid.uber.config.ConfiguracaoFirebase;
import com.cursoandroid.uber.helper.RecyclerItemClickListener;
import com.cursoandroid.uber.helper.UsuarioFirebase;
import com.cursoandroid.uber.model.Requisicao;
import com.cursoandroid.uber.model.Usuario;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class RequisicoesActivity extends AppCompatActivity {

    private LocationManager locationManager;
    private LocationListener locationListener;

    private FirebaseAuth autenticacao;
    private DatabaseReference firebaseRef;
    private List<Requisicao> listaRequisicoes = new ArrayList<>();
    private RequisicoesAdapter adapter;
    private Usuario motorista;

    private ActivityRequisicoesBinding  binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inicializarComponentes();
        recuperarLocalizacaoUsuario();

    }

    @Override
    protected void onStart() {
        super.onStart();
        verificaStatusRequisicao();
    }

    private void verificaStatusRequisicao(){
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicoesPesquisa = requisicoes.orderByChild("motorista/id")
                .equalTo(usuarioLogado.getId());

        requisicoesPesquisa.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds: snapshot.getChildren()){
                    Requisicao requisicao = ds.getValue(Requisicao.class);

                    if(requisicao.getStatus().equals(Requisicao.STATUS_A_CAMINHO)
                            || requisicao.getStatus().equals(Requisicao.STATUS_VIAGEM)
                            || requisicao.getStatus().equals(Requisicao.STATUS_FINALIZADA)
                    ){
                        motorista = requisicao.getMotorista();
                        abrirTelaCorrida(requisicao.getId(), motorista, true);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuSair:
                autenticacao.signOut();
                finish();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    private void abrirTelaCorrida(String idRequisicao, Usuario motorista, boolean requisicaoAtiva){

        Intent i = new Intent(RequisicoesActivity.this, CorridaActivity.class);
        i.putExtra("idRequisicao", idRequisicao);
        i.putExtra("motorista", motorista);
        i.putExtra("requisicaoAtiva", requisicaoAtiva);
        startActivity(i);
    }

    private void inicializarComponentes(){

        binding = ActivityRequisicoesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        getSupportActionBar().setTitle("Requisições");

        motorista = UsuarioFirebase.getDadosUsuarioLogado();
        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();


        adapter = new RequisicoesAdapter(listaRequisicoes, getApplicationContext(), motorista);
        RecyclerView.LayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        binding.rvRequisicoes.setLayoutManager(layoutManager);
        binding.rvRequisicoes.setHasFixedSize(true);
        binding.rvRequisicoes.setAdapter(adapter);

        recuperarRequisicoes();
    }

    private void adicionaEventoCliqueRecyclerView(){
        binding.rvRequisicoes.addOnItemTouchListener(new RecyclerItemClickListener(
                getApplicationContext(),
                binding.rvRequisicoes,
                new RecyclerItemClickListener.OnItemClickListener() {
                    @Override
                    public void onItemClick(View view, int position) {
                        Requisicao requisicao = listaRequisicoes.get(position);
                        abrirTelaCorrida(requisicao.getId(), motorista, false);

                    }

                    @Override
                    public void onLongItemClick(View view, int position) {

                    }

                    @Override
                    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {

                    }
                }
        ));
    }

    private void recuperarRequisicoes(){
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("status")
                .equalTo(Requisicao.STATUS_AGUARDANDO);
        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if(snapshot.getChildrenCount() >0){
                    binding.tvResultado.setVisibility(View.GONE);
                    binding.rvRequisicoes.setVisibility(View.VISIBLE);
                } else {
                    binding.tvResultado.setVisibility(View.VISIBLE);
                    binding.rvRequisicoes.setVisibility(View.GONE);
                }
                listaRequisicoes.clear();
                for(DataSnapshot ds: snapshot.getChildren()){
                    Requisicao requisicao = ds.getValue(Requisicao.class);
                    listaRequisicoes.add(requisicao);
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                String latitude = String.valueOf(location.getLatitude());
                String longitude = String.valueOf(location.getLongitude());

                UsuarioFirebase.atualizarDadosLocalizacao(location.getLatitude(), location.getLongitude());

                if(!latitude.isEmpty() && !longitude.isEmpty()){
                    motorista.setLatitude(latitude);
                    motorista.setLongitude(longitude);

                    adicionaEventoCliqueRecyclerView();
                    locationManager.removeUpdates(locationListener);
                    adapter.notifyDataSetChanged();
                }

            }
        };
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    0,
                    0,
                    locationListener
            );
        }
    }



}