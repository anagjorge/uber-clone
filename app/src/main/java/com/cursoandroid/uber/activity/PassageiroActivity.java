package com.cursoandroid.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;

import com.cursoandroid.uber.config.ConfiguracaoFirebase;
import com.cursoandroid.uber.databinding.ActivityPassageiroBinding;
import com.cursoandroid.uber.helper.Local;
import com.cursoandroid.uber.helper.UsuarioFirebase;
import com.cursoandroid.uber.model.Destino;
import com.cursoandroid.uber.model.Requisicao;
import com.cursoandroid.uber.model.Usuario;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.navigation.ui.AppBarConfiguration;


import com.cursoandroid.uber.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class PassageiroActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    // Lat-lon passageiro: -23.562791, -46.654668
    // Lat-lon destino: -23.556407, -46.654668
    // Lat-lon motorista: inicial -23.563196, -46.650607
    //intermediaria = -23.564801, -46.652196
    //final: -23.563136, -46.654247

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;

    private AppBarConfiguration appBarConfiguration;
    private FirebaseAuth autenticacao;
    private DatabaseReference firebaseRef;
    private Requisicao requisicao;

    private ActivityPassageiroBinding binding;

    private boolean cancelarUber = false;
    private LatLng localPassageiro;
    private Usuario passageiro;
    private String statusRequisicao;
    private Destino destino;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private Usuario motorista;
    private LatLng localMotorista;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inicializarComponentes();
        verificaStatusRequisicao();

    }

    private void verificaStatusRequisicao() {
        Usuario usuarioLogado = UsuarioFirebase.getDadosUsuarioLogado();
        DatabaseReference requisicoes = firebaseRef.child("requisicoes");
        Query requisicaoPesquisa = requisicoes.orderByChild("passageiro/id")
                .equalTo(usuarioLogado.getId());

        requisicaoPesquisa.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<Requisicao> lista = new ArrayList<>();
                //Log.d("resultado", "onDataChange: " + snapshot.toString());
                for (DataSnapshot ds : snapshot.getChildren()) {
                    requisicao = ds.getValue(Requisicao.class);
                    lista.add(requisicao);
                }
                Collections.reverse(lista);
                if (lista != null && lista.size() > 0) {
                    requisicao = lista.get(0);

                    if (requisicao != null) {
                        if(!requisicao.getStatus().equals(Requisicao.STATUS_ENCERRADA)){
                        passageiro = requisicao.getPassageiro();
                        localPassageiro = new LatLng(
                                Double.parseDouble(passageiro.getLatitude()),
                                Double.parseDouble(passageiro.getLongitude())
                        );
                        statusRequisicao = requisicao.getStatus();
                        destino = requisicao.getDestino();

                        if (requisicao.getMotorista() != null) {
                            motorista = requisicao.getMotorista();
                            localMotorista = new LatLng(
                                    Double.parseDouble(motorista.getLatitude()),
                                    Double.parseDouble(motorista.getLongitude())
                            );
                        }
                        alteraInterfaceStatusRequisicao(statusRequisicao);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void alteraInterfaceStatusRequisicao(String status) {

        if (status != null && !status.isEmpty()) {
            cancelarUber = false;
            switch (status) {
                case Requisicao.STATUS_AGUARDANDO:
                    requisicaoAguardando();
                    break;
                case Requisicao.STATUS_A_CAMINHO:
                    requisicaoACaminho();
                    break;
                case Requisicao.STATUS_VIAGEM:
                    requisicaoViagem();
                    break;
                case Requisicao.STATUS_FINALIZADA:
                    requisicaoFinalizada();
                    break;
                case Requisicao.STATUS_CANCELADA:
                    requisicaoCancelada();
                    break;
            }
        } else {
            adicionarMarcadorPassageiro(localPassageiro, "Seu local");
            centralizarMarcador(localPassageiro);
        }
    }

    private void requisicaoAguardando() {

        binding.llDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setText("Cancelar Uber");
        cancelarUber = true;

        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());
        centralizarMarcador(localPassageiro);


    }

    private void requisicaoACaminho() {

        binding.llDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setText("Motorista a caminho");
        binding.btnChamarUber.setEnabled(false);

        //Adiciona marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());

        //adiciona mrcador motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());
        centralizarMarcadores(marcadorMotorista, marcadorPassageiro);

    }

    private void requisicaoViagem() {
        binding.llDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setText("A caminho do destino");
        binding.btnChamarUber.setEnabled(false);

        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionaMarcadorDestino(localDestino, "Destino");
        centralizarMarcadores(marcadorMotorista, marcadorDestino);

    }

    private void requisicaoFinalizada() {
        binding.llDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setEnabled(false);

        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionaMarcadorDestino(localDestino, "Destino");
        centralizarMarcador(localDestino);

        float distancia = Local.calcularDistancia(localPassageiro, localDestino);
        float valor = distancia * 8;
        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);

        //binding.btnChamarUber.setText("Corrida finalizada: - " + resultado);

        AlertDialog.Builder builder = new AlertDialog.Builder(this)
                .setTitle("Total da viagem")
                .setMessage("Sua viagem ficou: R$ " + resultado)
                .setCancelable(false)
                .setNegativeButton("Encerrar viagem", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {

                        requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
                        requisicao.atualizarStatus();
                        finish();
                        startActivity(new Intent(getIntent()));
                    }
                });
        AlertDialog dialog = builder.create();
        builder.show();
    }

    private void requisicaoCancelada(){
        binding.llDestino.setVisibility(View.VISIBLE);
        binding.btnChamarUber.setText("Chamar Uber");
        cancelarUber = false;
    }

    private void adicionarMarcadorPassageiro(LatLng localizacao, String titulo) {
        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        marcadorPassageiro = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.usuario))
        );
    }

    private void adicionaMarcadorMotorista(LatLng localizacao, String titulo) {

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        marcadorMotorista = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.carro))
        );
    }

    private void adicionaMarcadorDestino(LatLng localizacao, String titulo) {

        if (marcadorPassageiro != null)
            marcadorPassageiro.remove();

        if (marcadorDestino != null)
            marcadorDestino.remove();

        marcadorDestino = mMap.addMarker(
                new MarkerOptions()
                        .position(localizacao)
                        .title(titulo)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.destino))
        );
    }


    private void centralizarMarcador(LatLng local) {

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(local, 20));
    }

    private void centralizarMarcadores(Marker marcador1, Marker marcador2) {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        builder.include(marcador1.getPosition());
        builder.include(marcador2.getPosition());

        LatLngBounds bounds = builder.build();

        int largura = getResources().getDisplayMetrics().widthPixels;
        int altura = getResources().getDisplayMetrics().heightPixels;
        int espacoInterno = (int) (largura * 0.20);

        mMap.moveCamera(
                CameraUpdateFactory.newLatLngBounds(bounds, largura, altura, espacoInterno)
        );
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        recuperarLocalizacaoUsuario();

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

    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localPassageiro = new LatLng(latitude, longitude);

                //Atualizar geofire
                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);
                alteraInterfaceStatusRequisicao(statusRequisicao);

                if (statusRequisicao != null && !statusRequisicao.isEmpty()) {
                    if (statusRequisicao.equals(Requisicao.STATUS_VIAGEM)
                            || statusRequisicao.equals(Requisicao.STATUS_FINALIZADA)) {
                        locationManager.removeUpdates(locationListener);
                    } else {
                        if (ActivityCompat.checkSelfPermission(PassageiroActivity.this, Manifest.permission.ACCESS_FINE_LOCATION)
                                == PackageManager.PERMISSION_GRANTED) {
                            locationManager.requestLocationUpdates(
                                    LocationManager.GPS_PROVIDER,
                                    10000,
                                    10,
                                    locationListener
                            );
                        }
                    }
                }
            }
        };
        //Solicitar atualizações de requisição
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    10000,
                    10,
                    locationListener
            );
        }
    }

    private Address recuperarEndereco(String endereco) {
        Geocoder geocoder = new Geocoder(this, Locale.getDefault());
        try {
            List<Address> listaEnderecos = geocoder.getFromLocationName(endereco, 1);
            if (listaEnderecos != null && listaEnderecos.size() > 0) {
                Address address = listaEnderecos.get(0);

                return address;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    public void chamarUber(View view) {

        //uber pode ser cancelado
        if (cancelarUber) {

            //cancelar a requisição
            requisicao.setStatus(Requisicao.STATUS_CANCELADA);
            requisicao.atualizarStatus();

        } else {
            String enderecoDestino = binding.etDestino.getText().toString();
            if (!enderecoDestino.equals("") || enderecoDestino != null) {
                Address addressDestino = recuperarEndereco(enderecoDestino);
                if (addressDestino != null) {

                    final Destino destino = new Destino();
                    destino.setCidade(addressDestino.getAdminArea());
                    destino.setCep(addressDestino.getPostalCode());
                    destino.setBairro(addressDestino.getSubLocality());
                    destino.setRua(addressDestino.getThoroughfare());
                    destino.setNumero(addressDestino.getFeatureName());
                    destino.setLatitude(String.valueOf(addressDestino.getLatitude()));
                    destino.setLongitude(String.valueOf(addressDestino.getLongitude()));

                    StringBuilder mensagem = new StringBuilder();
                    mensagem.append("Cidade: " + destino.getCidade());
                    mensagem.append("\nRua: " + destino.getRua());
                    mensagem.append("\nBairro: " + destino.getBairro());
                    mensagem.append("\nNúmero: " + destino.getNumero());
                    mensagem.append("\nCep: " + destino.getCep());

                    AlertDialog.Builder builder = new AlertDialog.Builder(this)
                            .setTitle("Confirme seu endereço!")
                            .setMessage(mensagem)
                            .setPositiveButton("Confirmar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    //salvar requisição
                                    salvarRequisicao(destino);
                                }
                            }).setNegativeButton("Cancelar", new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {

                                }
                            });
                    AlertDialog dialog = builder.create();
                    builder.show();
                }
            }
            Toast.makeText(this, "Informe o endereço de destino",
                    Toast.LENGTH_SHORT).show();


        }
    }

    private void salvarRequisicao(Destino destino) {

        Requisicao requisicao = new Requisicao();
        requisicao.setDestino(destino);

        Usuario usuarioPassageiro = UsuarioFirebase.getDadosUsuarioLogado();
        usuarioPassageiro.setLatitude(String.valueOf(localPassageiro.latitude));
        usuarioPassageiro.setLongitude(String.valueOf(localPassageiro.longitude));

        requisicao.setPassageiro(usuarioPassageiro);
        requisicao.setStatus(Requisicao.STATUS_AGUARDANDO);
        requisicao.salvar();

        binding.llDestino.setVisibility(View.GONE);
        binding.btnChamarUber.setText("Cancelar Uber");
    }

    private void inicializarComponentes() {
        binding = ActivityPassageiroBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        binding.toolbar.setTitle("Iniciar uma viagem");
        setSupportActionBar(binding.toolbar);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }


}