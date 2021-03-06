package projetoi.meucarro;

import android.app.ProgressDialog;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.toptoche.searchablespinnerlibrary.SearchableSpinner;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;


import projetoi.meucarro.models.Carro;
import projetoi.meucarro.models.Gasto;
import projetoi.meucarro.models.User;

public class AdicionarCarroFragment extends Fragment {

    private SearchableSpinner spinnerMarca;
    private SearchableSpinner spinnerModelo;
    private Spinner spinnerAno;

    private EditText placaLetras;

    private Button adicionarButton;
    private FirebaseAuth mAuth;
    private ArrayList<String> anoCarroList;
    private ArrayAdapter<String> adapterAno;
    private HashMap<String, List<Integer>> modeloMap;
    private User user;
    private ArrayList<String> carrosModeloList;
    private HashMap<String, String> carroModeloHash;
    private HashMap<String, String> carrosMarcaHash;
    private ArrayList<String> carroMarcaList;
    private ArrayAdapter<String> adapterMarca;
    private ArrayAdapter<String> adapterModelo;
    private ProgressDialog progressDialog;
    private EditText placaNumeros;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.activity_adicionar_carro, container, false);
        spinnerMarca = (SearchableSpinner) rootView.findViewById(R.id.adicionarCarroSpinnerMarca);
        spinnerModelo = (SearchableSpinner) rootView.findViewById(R.id.adicionarCarroSpinnerModelo);
        spinnerAno = (Spinner) rootView.findViewById(R.id.adicionarCarroSpinnerAno);

        placaLetras = (EditText) rootView.findViewById(R.id.editTextPlacaLetras);
        placaNumeros = (EditText) rootView.findViewById(R.id.editTextPlacaNumeros);


        mAuth = FirebaseAuth.getInstance();

        adicionarButton = (Button) rootView.findViewById(R.id.confirmaAdicionarCarro);

        carrosModeloList = new ArrayList<>();
        anoCarroList = new ArrayList<>();
        carrosMarcaHash = new HashMap<>();
        carroModeloHash = new HashMap<>();
        modeloMap = new HashMap<>();
        carroMarcaList = new ArrayList<>();

        adapterMarca = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, carroMarcaList);
        adapterModelo = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, carrosModeloList);
        adapterAno =  new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_item, anoCarroList);

        adapterMarca.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerMarca.setAdapter(adapterMarca);

        adapterModelo.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerModelo.setAdapter(adapterModelo);

        spinnerAno.setAdapter(adapterAno);

        spinnerMarca.setTitle("Escolha a marca");
        spinnerMarca.setPositiveButton("OK");

        spinnerModelo.setTitle("Escolha o modelo");
        spinnerModelo.setPositiveButton("OK");

        final FirebaseDatabase database = FirebaseDatabase.getInstance();
        final DatabaseReference ref = database.getReference();


        progressDialog = new ProgressDialog(getContext());
        progressDialog.setMessage("Carregando dados...");
        progressDialog.show();
        progressDialog.setCancelable(false);

        carregarListaMarca();

        ValueEventListener carregaUser = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                user = dataSnapshot.child("users").child(mAuth.getCurrentUser().getUid()).getValue(User.class);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.w("Nome", "loadPost:onCancelled", databaseError.toException());
            }
        };

        ref.addValueEventListener(carregaUser);

        spinnerMarca.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, final long id) {
                String marcaSelecionada = carrosMarcaHash.get(spinnerMarca.getSelectedItem().toString());
                carregaModelo(marcaSelecionada);
                progressDialog.show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        spinnerModelo.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String idMarca = carrosMarcaHash.get(spinnerMarca.getSelectedItem().toString());
                String idModelo = carroModeloHash.get(spinnerModelo.getSelectedItem().toString());
                carregarAnoList(idMarca, idModelo);
                progressDialog.show();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        adicionarButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String marcaSelecionada = spinnerMarca.getSelectedItem().toString();
                String modeloCarroSelecionado = spinnerModelo.getSelectedItem().toString();
                String modeloAnoSelecionado = spinnerAno.getSelectedItem().toString();
                String placaCarro = placaLetras.getText().toString().concat(placaNumeros.getText().toString());
                Carro carro = new Carro(marcaSelecionada, modeloCarroSelecionado, modeloAnoSelecionado, placaCarro, 0, new ArrayList<Gasto>());
                if (user.cars == null) {
                    user.cars = new ArrayList<>();
                } else if (placaLetras.getText().length() != 3) {
                    placaLetras.setError(getString(R.string.adicionarcarro_msgerroplacaletras));
                } else if (placaNumeros.getText().length() != 4) {
                    placaNumeros.setError(getString(R.string.adicionarcarro_msgerroplacanumeros));
                } else {
                    user.addCar(carro);
                    ref.child("users").child(mAuth.getCurrentUser().getUid()).setValue(user);

                    Toast.makeText(getContext(), R.string.adicionarcarro_msgsucesso, Toast.LENGTH_SHORT).show();
                    backToHome();
                }


            }
        });

        return rootView;
    }

    private void carregarListaMarca() {
        carrosMarcaHash.clear();
        carroMarcaList.clear();
        adapterMarca.notifyDataSetChanged();
        String url = "https://fipe.parallelum.com.br/api/v1/carros/marcas";
        StringRequest request = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                try {
                    JSONArray arrayAPI = new JSONArray(string);
                    for (int i = 0; i < arrayAPI.length(); i++) {
                        JSONObject marcaObj = arrayAPI.getJSONObject(i);
                        carrosMarcaHash.put(String.valueOf(marcaObj.get("nome")), String.valueOf(marcaObj.get("codigo")));
                    }
                    carroMarcaList.addAll(carrosMarcaHash.keySet());
                    adapterMarca.notifyDataSetChanged();
                    progressDialog.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });

        RequestQueue rQueue = Volley.newRequestQueue(getContext());
        rQueue.add(request);
    }

    private void carregaModelo(String marcaId) {
        String url = String.format("https://fipe.parallelum.com.br/api/v1/carros/marcas/%s/modelos", marcaId);
        carroModeloHash.clear();

        carrosModeloList.clear();
        adapterModelo.notifyDataSetChanged();
        StringRequest request = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                try {
                    JSONObject t = new JSONObject(string);
                    JSONArray arrayAPI = (JSONArray) t.get("modelos");
                    for (int i = 0; i < arrayAPI.length(); i++) {
                        JSONObject modeloObj = arrayAPI.getJSONObject(i);
                        carroModeloHash.put(String.valueOf(modeloObj.get("nome")), String.valueOf(modeloObj.get("codigo")));
                    }
                    carrosModeloList.addAll(carroModeloHash.keySet());
                    adapterModelo.notifyDataSetChanged();
                    progressDialog.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
        RequestQueue rQueue = Volley.newRequestQueue(getContext());
        rQueue.add(request);
    }

    private void carregarAnoList(String marcaId, String modeloId) {
        String url = String.format("https://fipe.parallelum.com.br/api/v1/carros/marcas/%s/modelos/%s/anos", marcaId, modeloId);
        anoCarroList.clear();
        adapterAno.notifyDataSetChanged();
        StringRequest request = new StringRequest(url, new Response.Listener<String>() {
            @Override
            public void onResponse(String string) {
                try {
                    JSONArray arrayAPI = new JSONArray(string);
                    for (int i = 0; i < arrayAPI.length(); i++) {
                        JSONObject modeloObj = arrayAPI.getJSONObject(i);
                        anoCarroList.add(String.valueOf(modeloObj.get("nome")));
                    }
                    adapterAno.notifyDataSetChanged();
                    progressDialog.dismiss();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {

            }
        });
        RequestQueue rQueue = Volley.newRequestQueue(getContext());
        rQueue.add(request);
    }

    public void backToHome() {
        HomeFragment homeFragment = new HomeFragment();
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.replace(R.id.content_frame, homeFragment);
        fragmentTransaction.commit();
    }

}
