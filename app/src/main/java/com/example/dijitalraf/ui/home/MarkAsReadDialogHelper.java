package com.example.dijitalraf.ui.home;

import androidx.fragment.app.Fragment;

import com.example.dijitalraf.R;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

/**
 * Okunan kitaplar listesine taşımadan önce kullanıcı onayı için diyalog.
 */
public final class MarkAsReadDialogHelper {

    private MarkAsReadDialogHelper() {
    }

    /**
     * @param markingAsRead true ise kitap okunacaklardan okunanlara alınacaktır; onay istenir.
     *                      false ise okunandan okunacağa dönüş; doğrudan çalıştırılır.
     */
    public static void runWithConfirmationIfMarkingRead(
            Fragment fragment,
            boolean markingAsRead,
            Runnable onConfirmed) {
        if (!markingAsRead) {
            onConfirmed.run();
            return;
        }
        new MaterialAlertDialogBuilder(fragment.requireContext())
                .setTitle(R.string.mark_as_read_dialog_title)
                .setMessage(R.string.mark_as_read_dialog_message)
                .setNegativeButton(R.string.dialog_cancel, (d, w) -> d.dismiss())
                .setPositiveButton(R.string.mark_as_read_dialog_confirm, (d, w) -> onConfirmed.run())
                .show();
    }
}
