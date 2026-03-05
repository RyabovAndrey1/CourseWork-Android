package ru.ryabov.studentperformance.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.ryabov.studentperformance.data.local.entity.GroupEntity
import ru.ryabov.studentperformance.data.local.entity.StudentEntity
import ru.ryabov.studentperformance.data.repository.StudentRepository

class GroupDetailViewModel(
    private val repository: StudentRepository
) : ViewModel() {

    private val _group = MutableLiveData<GroupEntity?>()
    val group: LiveData<GroupEntity?> = _group

    private val _students = MutableLiveData<List<StudentEntity>>(emptyList())
    val students: LiveData<List<StudentEntity>> = _students

    fun loadGroupAndStudents(groupId: Long) {
        viewModelScope.launch {
            _group.value = repository.getGroupById(groupId)
            repository.getStudentsByGroup(groupId).collectLatest { list ->
                _students.value = list
            }
        }
    }

    class Factory(private val repository: StudentRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(GroupDetailViewModel::class.java)) {
                return GroupDetailViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel: $modelClass")
        }
    }
}
