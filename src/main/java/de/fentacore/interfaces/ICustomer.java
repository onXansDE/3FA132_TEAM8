package de.fentacore.interfaces;

import java.time.LocalDate;

public interface ICustomer extends IId {

   enum Gender {
      D,
      M,
      U,
      W
   }

   LocalDate getBirthDate();

   String getFirstName();

   Gender getGender();

   String getLastName();

   void setBirthDate(LocalDate birtDate);

   void setFirstName(String firstName);

   void setGender(Gender gender);

   void setLastName(String lastName);

}
