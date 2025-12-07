package com.example.blottermanagementsystem.data.dao;

import androidx.room.*;
import com.example.blottermanagementsystem.data.entity.Person;
import java.util.List;

@Dao
public interface PersonDao {
    @Query("SELECT * FROM person WHERE isActive = 1 ORDER BY lastName, firstName")
    List<Person> getAllActivePersons();
    
    @Query("SELECT * FROM person WHERE id = :personId")
    Person getPersonById(int personId);
    
    @Query("SELECT * FROM person WHERE personType = :type AND isActive = 1")
    List<Person> getPersonsByType(String type);
    
    @Query("SELECT * FROM person WHERE (firstName || ' ' || lastName) = :fullName AND isActive = 1 LIMIT 1")
    Person getPersonByName(String fullName);
    
    // ✅ Case-insensitive matching (HYBRID APPROACH - Option 1)
    @Query("SELECT * FROM person WHERE LOWER(TRIM(firstName || ' ' || lastName)) = LOWER(TRIM(:fullName)) AND isActive = 1 LIMIT 1")
    Person getPersonByNameIgnoreCase(String fullName);
    
    // ✅ Search for similar names (HYBRID APPROACH - Option 3)
    @Query("SELECT * FROM person WHERE LOWER(firstName || ' ' || lastName) LIKE '%' || LOWER(:searchTerm) || '%' AND isActive = 1 ORDER BY firstName, lastName")
    List<Person> searchPersonByNameSimilar(String searchTerm);
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertPerson(Person person);
    
    @Update
    void updatePerson(Person person);
    
    @Delete
    void deletePerson(Person person);
}
