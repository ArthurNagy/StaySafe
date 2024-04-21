package com.arthurnagy.staysafe.feature.newdocument

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import com.arthurnagy.staysafe.core.ResultWrapper
import com.arthurnagy.staysafe.core.StatementLocalSource
import com.arthurnagy.staysafe.core.model.Motive
import com.arthurnagy.staysafe.core.model.Statement
import com.arthurnagy.staysafe.feature.shared.mediatorLiveData
import com.google.android.material.datepicker.MaterialDatePicker
import timber.log.Timber

class NewDocumentViewModel(private val statementLocalSource: StatementLocalSource) : ViewModel() {

    private val lastSavedStatement: LiveData<Statement?> = liveData<Statement?> {
        when (val result = statementLocalSource.getLast()) {
            is ResultWrapper.Success -> emit(result.value)
            is ResultWrapper.Error -> Timber.e(result.exception)
        }
    }
    val hasExistingSignature: LiveData<Boolean> = lastSavedStatement.map { it?.signaturePath != null }
    val existingSignaturePath: LiveData<String?> = lastSavedStatement.map { it?.signaturePath }

    private val _pendingStatement: MutableLiveData<PendingStatement> = mediatorLiveData(
        defaultValue = PendingStatement(),
        dependency = lastSavedStatement,
        onChanged = { pendingDocument: PendingStatement?, lastSavedDocument: Statement? ->
            transformLastSavedStatementToPendingStatement(pendingDocument, lastSavedDocument)
        })
    val pendingStatement: LiveData<PendingStatement> get() = _pendingStatement

    fun updateStatement(func: PendingStatement.() -> PendingStatement?) {
        _pendingStatement.value = _pendingStatement.value?.func()
    }

    private fun transformLastSavedStatementToPendingStatement(pendingStatement: PendingStatement?, statement: Statement?): PendingStatement? =
        pendingStatement?.copy(
            firstName = statement?.firstName ?: pendingStatement.firstName,
            lastName = statement?.lastName ?: pendingStatement.lastName,
            birthDate = statement?.birthDate ?: pendingStatement.birthDate,
            location = statement?.location ?: pendingStatement.location,
            currentLocation = statement?.currentLocation ?: pendingStatement.currentLocation,
            birthdayLocation = statement?.birthdayLocation ?: pendingStatement.birthdayLocation,
            workLocation = statement?.workLocation ?: pendingStatement.workLocation,
            workAddresses = statement?.workAddresses ?: pendingStatement.workAddresses,
            date = MaterialDatePicker.todayInUtcMilliseconds()
        )

    data class PendingStatement(
        val firstName: String? = null,
        val lastName: String? = null,
        val birthDate: Long? = null,
        val location: String? = null,
        val currentLocation: String? = null,
        val birthdayLocation: String? = null,
        val workLocation: String? = null,
        val workAddresses: String? = null,
        val motives: List<Motive>? = null,
        val date: Long? = null,
        val signaturePath: String? = null,
        val restrictionStartHour: Int? = null
    )
}