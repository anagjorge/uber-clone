package com.cursoandroid.uber.activity;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;

import com.cursoandroid.uber.config.ConfiguracaoFirebase;
import com.cursoandroid.uber.databinding.ActivityCorridaBinding;
import com.cursoandroid.uber.helper.Local;
import com.cursoandroid.uber.helper.UsuarioFirebase;
import com.cursoandroid.uber.model.Destino;
import com.cursoandroid.uber.model.Requisicao;
import com.cursoandroid.uber.model.Usuario;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.firebase.geofire.GeoQuery;
import com.firebase.geofire.GeoQueryEventListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;


import com.cursoandroid.uber.R;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;

public class CorridaActivity extends AppCompatActivity
        implements OnMapReadyCallback {

    private GoogleMap mMap;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private LatLng localMotorista;
    private LatLng localPassageiro;
    private Usuario motorista;
    private Usuario passageiro;
    private String idRequisicao;
    private Requisicao requisicao;
    private DatabaseReference firebaseRef;
    private Marker marcadorMotorista;
    private Marker marcadorPassageiro;
    private Marker marcadorDestino;
    private String statusRequisicao;
    private Boolean requisicaoAtiva;

    private Destino destino;

    private ActivityCorridaBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        inicializarComponentes();

        //Recupera dados do usuário
        if (getIntent().getExtras().containsKey("idRequisicao")
                && getIntent().getExtras().containsKey("motorista")) {
            Bundle extras = getIntent().getExtras();
            motorista = (Usuario) extras.getSerializable("motorista");
            localMotorista = new LatLng(
                    Double.parseDouble(motorista.getLatitude()),
                    Double.parseDouble(motorista.getLongitude())
            );
            idRequisicao = extras.getString("idRequisicao");
            requisicaoAtiva = extras.getBoolean("requisicaoAtiva");
            verificaStatusRequisicao();

        }
    }

    private void verificaStatusRequisicao() {

        DatabaseReference requisicoes = firebaseRef.child("requisicoes")
                .child(idRequisicao);
        requisicoes.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                requisicao = snapshot.getValue(Requisicao.class);
                if (requisicao != null) {
                    passageiro = requisicao.getPassageiro();
                    localPassageiro = new LatLng(
                            Double.parseDouble(passageiro.getLatitude()),
                            Double.parseDouble(passageiro.getLongitude())
                    );
                    statusRequisicao = requisicao.getStatus();
                    destino = requisicao.getDestino();
                    alteraInterfaceStatusRequisicao(statusRequisicao);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void alteraInterfaceStatusRequisicao(String status) {
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
    }


    private void requisicaoAguardando() {
        binding.btnAceitarCorrida.setText("Aceitar Corrida");

        adicionaMarcadorMotorista(localMotorista, motorista.getNome());
        centralizarMarcador(localMotorista);
    }

    private void requisicaoACaminho() {
        binding.btnAceitarCorrida.setText("A caminho do passageiro");
        binding.fabRota.setVisibility(View.VISIBLE);

        //exibe marcador motorista
        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //exibe marcador passageiro
        adicionarMarcadorPassageiro(localPassageiro, passageiro.getNome());


        centralizarMarcadores(marcadorMotorista, marcadorPassageiro);

        //inicia monitoramento do motorista/passageiro

        iniciarMonitoramento(motorista, localPassageiro, Requisicao.STATUS_VIAGEM);
    }

    private void requisicaoViagem() {
        binding.fabRota.setVisibility(View.VISIBLE);
        binding.btnAceitarCorrida.setText("A caminho do destino");

        adicionaMarcadorMotorista(localMotorista, motorista.getNome());

        //Exibe marcador destino
        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );

        adicionaMarcadorDestino(localDestino, "Destino");
        centralizarMarcadores(marcadorMotorista, marcadorDestino);
        iniciarMonitoramento(motorista, localDestino, Requisicao.STATUS_FINALIZADA);

    }

    private void requisicaoFinalizada() {
        binding.fabRota.setVisibility(View.GONE);
        requisicaoAtiva = false;

        if (marcadorMotorista != null)
            marcadorMotorista.remove();

        if (marcadorDestino != null)
            marcadorDestino.remove();

        LatLng localDestino = new LatLng(
                Double.parseDouble(destino.getLatitude()),
                Double.parseDouble(destino.getLongitude())
        );
        adicionaMarcadorDestino(localDestino, "Destino");
        centralizarMarcador(localDestino);

        //Calcular distância para obter valor
        float distancia = Local.calcularDistancia(localPassageiro, localDestino);
        float valor = distancia * 8; //4.56
        DecimalFormat decimal = new DecimalFormat("0.00");
        String resultado = decimal.format(valor);

        binding.btnAceitarCorrida.setText("Corrida Finalizada - R$ " + resultado);
    }

    private void requisicaoCancelada(){
        Toast.makeText(this, "Requisição foi cancelada pelo passageiro", Toast.LENGTH_SHORT).show();
        startActivity(new Intent(CorridaActivity.this, RequisicoesActivity.class));
    }

    private void iniciarMonitoramento(Usuario uOrigem, LatLng localDestino, String status) {
        DatabaseReference localUsuario = ConfiguracaoFirebase.getFirebaseDatabase()
                .child("local_usuario");
        GeoFire geofire = new GeoFire(localUsuario);

        //Adiciona círculo no passageiro
        Circle circulo = mMap.addCircle(
                new CircleOptions()
                        .center(localDestino)
                        .radius(50) //em metros
                        .fillColor(Color.argb(90, 255, 153, 0))
                        .strokeColor(Color.argb(190, 255, 152, 0))
        );
        //Recupera marcadores da área
        GeoQuery geoQuery = geofire.queryAtLocation(
                new GeoLocation(localDestino.latitude, localDestino.longitude),
                0.05 //em km(0.05 500 metros)
        );
        geoQuery.addGeoQueryEventListener(new GeoQueryEventListener() {
            @Override
            public void onKeyEntered(String key, GeoLocation location) {
               /* if(key.equals(passageiro.getId())){
                    Log.d("onKeyEntered", "onKeyEntered: passageiro dentro da área");
                } else if(key.equals(motorista.getId())){
                    Log.d("onKeyEntered", "onKeyEntered: motorista dentro da área");
                }*/

                if (key.equals(uOrigem.getId())) {

                    //altera status da requisicao
                    requisicao.setStatus(status);
                    requisicao.atualizarStatus();

                    geoQuery.removeAllListeners();
                    circulo.remove();
                }
            }

            @Override
            public void onKeyExited(String key) {

            }

            @Override
            public void onKeyMoved(String key, GeoLocation location) {

            }

            @Override
            public void onGeoQueryReady() {

            }

            @Override
            public void onGeoQueryError(DatabaseError error) {

            }
        });
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


    public void aceitarCorrida(View view) {
        requisicao = new Requisicao();
        requisicao.setId(idRequisicao);
        requisicao.setMotorista(motorista);
        requisicao.setStatus(Requisicao.STATUS_A_CAMINHO);

        requisicao.atualizar();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;
        recuperarLocalizacaoUsuario();
    }

    private void recuperarLocalizacaoUsuario() {
        locationManager = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();
                localMotorista = new LatLng(latitude, longitude);

                UsuarioFirebase.atualizarDadosLocalizacao(latitude, longitude);
                motorista.setLatitude(String.valueOf(latitude));
                motorista.setLongitude(String.valueOf(longitude));
                requisicao.setMotorista(motorista);
                requisicao.atualizarLocalizacaoMotorista();

                alteraInterfaceStatusRequisicao(statusRequisicao);

            }
        };
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

    private void inicializarComponentes() {

        binding = ActivityCorridaBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setTitle("Iniciar corrida");

        firebaseRef = ConfiguracaoFirebase.getFirebaseDatabase();

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        binding.fabRota.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String status = statusRequisicao;
                if (status != null && !status.isEmpty()) {
                    String lat = "";
                    String lon = "";

                    switch (status) {
                        case Requisicao.STATUS_A_CAMINHO:
                            lat = String.valueOf(localPassageiro.latitude);
                            lon = String.valueOf(localPassageiro.longitude);
                            break;

                        case Requisicao.STATUS_VIAGEM:
                            lat = destino.getLatitude();
                            lon = destino.getLongitude();
                            break;
                    }
                    //Abrir rota
                    String latLong = lat + "," + lon;
                    Uri uri = Uri.parse("google.navigation:q=" + latLong + "&mode=d");
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, uri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    startActivity(mapIntent);

                }
            }
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        if (requisicaoAtiva) {
            Toast.makeText(CorridaActivity.this, "Necessário encerrar a requisição atual!",
                    Toast.LENGTH_SHORT).show();
        } else {
            Intent i = new Intent(CorridaActivity.this, RequisicoesActivity.class);
            startActivity(i);
        }
        if (statusRequisicao != null && !statusRequisicao.isEmpty()) {
            requisicao.setStatus(Requisicao.STATUS_ENCERRADA);
            requisicao.atualizarStatus();
        }
        return false;
    }
}
