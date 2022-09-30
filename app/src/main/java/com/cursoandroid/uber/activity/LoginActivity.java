package com.cursoandroid.uber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.Manifest;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;
import android.widget.Toast;

import com.cursoandroid.uber.R;
import com.cursoandroid.uber.config.ConfiguracaoFirebase;
import com.cursoandroid.uber.helper.UsuarioFirebase;
import com.cursoandroid.uber.model.Usuario;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;

public class LoginActivity extends AppCompatActivity {
    private TextInputEditText et_email, et_senha;

    private FirebaseAuth autenticacao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        inicializarComponentes();
    }

    private void inicializarComponentes(){
        et_email = findViewById(R.id.et_email_login);
        et_senha = findViewById(R.id.et_senha_login);
    }

    public void validarLoginUsuario(View view){
        String email = et_email.getText().toString();
        String senha = et_senha.getText().toString();

        if(!email.isEmpty()){
            if(!senha.isEmpty()){
                Usuario usuario = new Usuario();
                usuario.setEmail(email);
                usuario.setSenha(senha);

                logarUsuario(usuario);
            } else {
                Toast.makeText(this, "Preencha a senha", Toast.LENGTH_SHORT).show();
            }

        } else {
            Toast.makeText(this, "Preencha o email", Toast.LENGTH_SHORT).show();
        }
    }
    public void logarUsuario(Usuario usuario){

        autenticacao = ConfiguracaoFirebase.getFirebaseAutenticacao();
        autenticacao.signInWithEmailAndPassword(
                usuario.getEmail(), usuario.getSenha()
        ).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if(task.isSuccessful()){
                    UsuarioFirebase.redirecionaUsuarioLogado(LoginActivity.this);

                }else{
                    String excecao = "";
                    try{
                        throw task.getException();
                    } catch (FirebaseAuthInvalidUserException e){
                        excecao = "Usuario não está cadastrado";
                    } catch (FirebaseAuthInvalidCredentialsException e ){
                        excecao = "E-mail e senha incorretos";
                    } catch (Exception e){
                        excecao = "Erro ao logar usuário: " + e.getMessage();
                        e.printStackTrace();
                    }
                    Toast.makeText(LoginActivity.this, excecao, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


}