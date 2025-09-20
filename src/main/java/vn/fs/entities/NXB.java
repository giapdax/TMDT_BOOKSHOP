package vn.fs.entities;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Table(name = "nxb")
public class NXB {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")         // PK duy nhất
    private Long id;

    @Column(name = "name")
    private String name;

    // Nếu bảng nxb có cột status (tinyint(1))
    @Column(name = "status")
    private Boolean status;
}
