package projetoi.meucarro.dialog;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import projetoi.meucarro.R;
import projetoi.meucarro.models.Carro;
import projetoi.meucarro.models.Gasto;
import projetoi.meucarro.models.GastoCombustivel;
import projetoi.meucarro.models.User;
import projetoi.meucarro.utils.CheckStatus;
import projetoi.meucarro.utils.StatusAdapterPlaceholder;

public class AdicionarGastoDialog extends Dialog {


    private Date dataEscolhida;
    private Carro carro;
    private FirebaseAuth mAuth;
    private EditText editTextValor;
    private EditText editTextKm;
    private EditText editTextValorUnidadeCombustivel;
    private Spinner dialogSpinner;
    private Button dataButton;
    private Button adcButton;
    private HashMap manutencaoHash;
    private User user;

    public AdicionarGastoDialog(Activity activity) {
        super(activity);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setContentView(R.layout.dialog_adicionargasto);
        final Calendar dataAtual = Calendar.getInstance();

        mAuth = FirebaseAuth.getInstance();

        dataButton = (Button) findViewById(R.id.dialogDataButton);
        adcButton = (Button) findViewById(R.id.dialogAdicionar);
        dialogSpinner = (Spinner) findViewById(R.id.dialogSpinner);
        editTextValor = (EditText) findViewById(R.id.dialogValorEdit);
        editTextKm = (EditText) findViewById(R.id.quilometragemEdit);
        editTextValorUnidadeCombustivel = (EditText) findViewById(R.id.dialogValorUnidadeCombustivelEditText);


        ArrayAdapter dialogAdapter = ArrayAdapter.createFromResource(getContext(), R.array.adicionardialog_gastosarray,
                android.R.layout.simple_spinner_item);

        dialogSpinner.setAdapter(dialogAdapter);

        final int mesCorrigido = dataAtual.get(Calendar.MONTH) + 1;

        dataButton.setText(dataAtual.get(Calendar.DAY_OF_MONTH) +"/"+mesCorrigido+"/"+
                dataAtual.get(Calendar.YEAR));
        dataEscolhida = dataAtual.getTime();

        dataButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                final DatePickerDialog.OnDateSetListener listener = new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                        Calendar pagamento = Calendar.getInstance();
                        pagamento.set(year, month, dayOfMonth);
                        pagamento.set(Calendar.HOUR_OF_DAY, 0);
                        pagamento.set(Calendar.MINUTE, 30);
                        if (pagamento.compareTo(dataAtual) > 0) {
                            pagamento = Calendar.getInstance();
                            Toast.makeText(getContext(), R.string.erro_adicionargasto_dataposterior,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            dataButton.setText(dayOfMonth+"/"+ (month + 1)+"/"+year);
                        }
                        dataEscolhida = pagamento.getTime();

                    }
                };

                DatePickerDialog datePickerDialog = new DatePickerDialog(
                        getContext(), listener, dataAtual.get(Calendar.YEAR), dataAtual.get(Calendar.MONTH),
                        dataAtual.get(Calendar.DAY_OF_MONTH));
                datePickerDialog.show();

            }
        });

        adcButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (editTextValor.getText().toString().isEmpty() || editTextKm.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), R.string.erro_adicionargasto_vazio,
                            Toast.LENGTH_SHORT).show();
                } else if (editTextValorUnidadeCombustivel.isEnabled() &&
                        editTextValorUnidadeCombustivel.getText().toString().isEmpty()) {
                    Toast.makeText(getContext(), "Valor do litro/m³ do combustível não pode ser vazio",
                            Toast.LENGTH_SHORT).show();
                } else {
                    Calendar dia = Calendar.getInstance();
                    dia.set(Calendar.HOUR_OF_DAY, 0);
                    dia.set(Calendar.MINUTE, 30);


                    if (dia.getTime().compareTo(dataEscolhida) > 0) {
                        int quilometragemNova = Integer.valueOf(editTextKm.getText().toString());
                            if (quilometragemNova > carro.kmRodados && carro.kmRodados != 0) {
                            Toast.makeText(getContext(), R.string.erro_adicionargasto_quilmetragem_maior,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            adicionaGasto(quilometragemNova);
                        }
                    } else {
                        int quilometragemNova = Integer.valueOf(editTextKm.getText().toString());
                        if (quilometragemNova < carro.kmRodados) {
                            Toast.makeText(getContext(), R.string.erro_adicionargasto_quilometragem_menor,
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            adicionaGasto(quilometragemNova);
                        }
                    }
                }
            }
        });

        dialogSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String combustivelString = getContext().getResources().getStringArray(R.array.adicionardialog_gastosarray)[0];
                if (dialogSpinner.getSelectedItem().toString().equals(combustivelString)) {
                    editTextValorUnidadeCombustivel.setEnabled(true);
                } else {
                    editTextValorUnidadeCombustivel.setEnabled(false);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });

        super.onCreate(savedInstanceState);
    }

    private void showNotif(Carro carro, HashMap manutencaoHash) {
        ArrayList<StatusAdapterPlaceholder> list = new ArrayList<>();
        list.addAll(CheckStatus.checaStatus(manutencaoHash, carro));
        for (StatusAdapterPlaceholder i : list) {
            Log.d("Atraso", String.valueOf(i.isAtrasado()));
            if (i.isAtrasado()) {
                NotificationCompat.Builder mBuilder =
                        new NotificationCompat.Builder(getContext())
                                .setContentTitle("MeuCarro - Atraso")
                                .setSmallIcon(android.R.drawable.ic_popup_reminder)
                                .setContentText(i.getManutencao()   );
                NotificationManager mNotificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(0, mBuilder.build());
            }
        }
    }

    private void adicionaGasto(int quilometragemNova) {

        Gasto novoGasto;
        if (editTextValorUnidadeCombustivel.isEnabled()) {
            novoGasto = new GastoCombustivel(dialogSpinner.getSelectedItem().toString(), dataEscolhida,
                    Float.valueOf(editTextValor.getText().toString()), quilometragemNova,
                    Float.valueOf(editTextValorUnidadeCombustivel.getText().toString()));
        } else {
            novoGasto = new Gasto(dialogSpinner.getSelectedItem().toString(), dataEscolhida,
                    Float.valueOf(editTextValor.getText().toString()), quilometragemNova);
        }
        Log.d("checa", String.valueOf(checaGastoMenor(novoGasto, carro.listaGastos)));
        if (!checaGastoMenor(novoGasto, carro.listaGastos)){
            Toast.makeText(getContext(), "Já existe um registro com quilometragem maior em dia anterior.",
                    Toast.LENGTH_SHORT).show();
        } else {

            if (carro.listaGastos == null) {
                carro.listaGastos = new ArrayList<>();
            }
            if (quilometragemNova >= carro.kmRodados) {
                carro.setKmRodados(quilometragemNova);
            }

            user.cars.get(user.lastCarIndex).adicionaGasto(novoGasto);
            Collections.sort(carro.listaGastos, Gasto.compareByData());
            saveUser(user);

            showNotif(carro, manutencaoHash);

            dismiss();
        }
    }

    private void saveUser(User user) {
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference().child("users");
        ref.child(mAuth.getCurrentUser().getUid()).setValue(user);
    }

    public void setInfo(User user, HashMap manutencaoHash) {
        this.user = user;
        this.carro = user.currentCar();
        this.manutencaoHash = manutencaoHash;
    }

    private boolean checaGastoMenor(Gasto gasto, List<Gasto> listGastos) {
        boolean podeAdicionar = true;
        Gasto ultimoGasto = null;

        if (listGastos != null) {
            for (Gasto g : listGastos) {
                if (g.data.compareTo(gasto.data) < 0) {
                    ultimoGasto = g;
                }
            }
        }

        if (ultimoGasto != null) {
            if (gasto.registroKm < ultimoGasto.registroKm) {
                Log.d("gastoRegistro", String.valueOf(gasto.registroKm));
                Log.d("ultimoRegistro", String.valueOf(ultimoGasto.registroKm));

                podeAdicionar = false;
            }
        }

        return podeAdicionar;
    }


}
