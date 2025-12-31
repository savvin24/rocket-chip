	.file	"sssp.cpp"
	.option nopic
	.attribute arch, "rv64i2p0_m2p0_a2p0_f2p0_d2p0_c2p0"
	.attribute unaligned_access, 0
	.attribute stack_align, 16
	.text
	.section	.rodata.str1.8,"aMS",@progbits,1
	.align	3
.LC0:
	.string	"Read graph %s\n"
	.align	3
.LC1:
	.string	"r"
	.align	3
.LC2:
	.string	"%d"
	.align	3
.LC3:
	.string	"Rows = %d Non-zero = %d \n"
	.align	3
.LC4:
	.string	"%f"
	.text
	.align	1
	.globl	_Z10read_graphPc
	.type	_Z10read_graphPc, @function
_Z10read_graphPc:
.LFB976:
	.cfi_startproc
	addi	sp,sp,-112
	.cfi_def_cfa_offset 112
	sd	s4,64(sp)
	.cfi_offset 20, -48
	mv	s4,a0
	lui	a0,%hi(.LC0)
	addi	a0,a0,%lo(.LC0)
	sd	ra,104(sp)
	sd	s0,96(sp)
	sd	s1,88(sp)
	.cfi_offset 1, -8
	.cfi_offset 8, -16
	.cfi_offset 9, -24
	mv	s0,a1
	sd	s2,80(sp)
	sd	s3,72(sp)
	sd	s5,56(sp)
	sd	s6,48(sp)
	sd	s7,40(sp)
	sd	s8,32(sp)
	.cfi_offset 18, -32
	.cfi_offset 19, -40
	.cfi_offset 21, -56
	.cfi_offset 22, -64
	.cfi_offset 23, -72
	.cfi_offset 24, -80
	call	printf
	lui	a1,%hi(.LC1)
	addi	a1,a1,%lo(.LC1)
	mv	a0,s0
	call	fopen
	lui	s7,%hi(.LC2)
	addi	a2,sp,12
	addi	a1,s7,%lo(.LC2)
	mv	s3,a0
	call	fscanf
	addi	a2,sp,16
	addi	a1,s7,%lo(.LC2)
	mv	a0,s3
	call	fscanf
	lw	a2,16(sp)
	lw	a1,12(sp)
	lui	a0,%hi(.LC3)
	addi	a0,a0,%lo(.LC3)
	call	printf
	lw	s5,12(sp)
	lw	s8,16(sp)
	addiw	s6,s5,1
	slli	s6,s6,2
	mv	a0,s6
	slli	s1,s8,2
	sw	s5,28(s4)
	sw	s8,32(s4)
	call	malloc
	mv	s0,a0
	mv	a0,s1
	sd	s0,0(s4)
	call	malloc
	mv	s2,a0
	mv	a0,s1
	sd	s2,8(s4)
	call	malloc
	sd	a0,16(s4)
	sw	s5,24(s4)
	mv	s1,a0
	blt	s5,zero,.L2
	li	s8,0
.L3:
	addi	a2,sp,20
	addi	a1,s7,%lo(.LC2)
	mv	a0,s3
	call	fscanf
	lw	a4,20(sp)
	lw	a5,12(sp)
	addiw	s8,s8,1
	sw	a4,0(s0)
	addi	s0,s0,4
	bge	a5,s8,.L3
	lw	a5,16(sp)
	ble	a5,zero,.L8
.L4:
	li	s0,0
.L7:
	addi	a2,sp,24
	addi	a1,s7,%lo(.LC2)
	mv	a0,s3
	call	fscanf
	lw	a4,24(sp)
	lw	a5,16(sp)
	addiw	s0,s0,1
	sw	a4,0(s2)
	addi	s2,s2,4
	bgt	a5,s0,.L7
	ble	a5,zero,.L8
	li	s0,0
	lui	s2,%hi(.LC4)
.L9:
	addi	a2,sp,28
	addi	a1,s2,%lo(.LC4)
	mv	a0,s3
	call	fscanf
	flw	fa5,28(sp)
	lw	a5,16(sp)
	addi	s1,s1,4
	fcvt.w.s a4,fa5,rtz
	addiw	s0,s0,1
	sw	a4,-4(s1)
	bgt	a5,s0,.L9
.L8:
	addi	a0,s6,-4
	call	malloc
	lui	a5,%hi(distance)
	sd	a0,%lo(distance)(a5)
	beq	s5,zero,.L1
.L5:
	slli	a5,s5,32
	srli	a2,a5,30
	li	a1,255
	call	memset
.L1:
	ld	ra,104(sp)
	.cfi_remember_state
	.cfi_restore 1
	ld	s0,96(sp)
	.cfi_restore 8
	ld	s1,88(sp)
	.cfi_restore 9
	ld	s2,80(sp)
	.cfi_restore 18
	ld	s3,72(sp)
	.cfi_restore 19
	ld	s5,56(sp)
	.cfi_restore 21
	ld	s6,48(sp)
	.cfi_restore 22
	ld	s7,40(sp)
	.cfi_restore 23
	ld	s8,32(sp)
	.cfi_restore 24
	mv	a0,s4
	ld	s4,64(sp)
	.cfi_restore 20
	addi	sp,sp,112
	.cfi_def_cfa_offset 0
	jr	ra
.L2:
	.cfi_restore_state
	bgt	s8,zero,.L4
	addi	a0,s6,-4
	call	malloc
	lui	a5,%hi(distance)
	sd	a0,%lo(distance)(a5)
	j	.L5
	.cfi_endproc
.LFE976:
	.size	_Z10read_graphPc, .-_Z10read_graphPc
	.section	.rodata.str1.8
	.align	3
.LC5:
	.string	"X= %d \n"
	.text
	.align	1
	.globl	_Z4ssspv
	.type	_Z4ssspv, @function
_Z4ssspv:
.LFB977:
	.cfi_startproc
	lui	t4,%hi(.LANCHOR0)
	addi	sp,sp,-32
	.cfi_def_cfa_offset 32
	addi	t4,t4,%lo(.LANCHOR0)
	lui	a5,%hi(distance)
	sd	s0,24(sp)
	sd	s1,16(sp)
	ld	t3,%lo(distance)(a5)
	.cfi_offset 8, -8
	.cfi_offset 9, -16
	ld	s0,0(t4)
	ld	a6,8(t4)
	ld	s1,16(t4)
	sd	s2,8(sp)
	sd	s3,0(sp)
	.cfi_offset 18, -24
	.cfi_offset 19, -32
	li	s2,100
	li	a1,0
	lui	t6,%hi(stride)
	li	t5,-1
.L18:
	lw	a4,28(t4)
	li	a5,0
	beq	a4,zero,.L27
.L19:
	slli	a3,a5,32
	srli	a4,a3,30
	add	a4,t3,a4
	negw	a3,a5
	sw	a3,0(a4)
	lw	a4,28(t4)
	addiw	a5,a5,1
	bgtu	a4,a5,.L19
.L27:
	sw	zero,0(t3)
	lw	a5,28(t4)
	beq	a5,zero,.L20
	li	t2,0
.L25:
	slli	a5,t2,32
	addiw	a4,t2,1
	srli	t1,a5,30
	slli	a5,a4,32
	srli	t0,a5,30
	add	t0,s0,t0
	add	a5,s0,t1
	lw	a5,0(a5)
	lw	a7,0(t0)
	sext.w	t2,a4
	bgeu	a5,a7,.L21
	slli	a0,a5,2
	add	t1,t3,t1
	add	a0,s1,a0
.L24:
	slli	a4,a5,2
	add	a3,a6,a4
	lw	a4,%lo(stride)(t6)
	lw	s3,32(t4)
	add	a4,a4,a5
	slli	a2,a4,2
	add	a2,a6,a2
	addi	a5,a5,1
	bgeu	a4,s3,.L22
	lw	a4,0(a2)
	slli	a4,a4,2
	add	a4,t3,a4
	lw	a4,0(a4)
	addw	a1,a4,a1
.L22:
	lw	a2,0(t1)
	beq	a2,t5,.L23
	lw	a4,0(a3)
	lw	a3,0(a0)
	slli	a4,a4,2
	add	a4,t3,a4
	lw	s3,0(a4)
	addw	a2,a3,a2
	ble	s3,a2,.L23
	sw	a2,0(a4)
	lw	a7,0(t0)
.L23:
	addi	a0,a0,4
	bgtu	a7,a5,.L24
.L21:
	lw	a5,28(t4)
	bgtu	a5,t2,.L25
.L20:
	addiw	s2,s2,-1
	bne	s2,zero,.L18
	ld	s0,24(sp)
	.cfi_restore 8
	ld	s1,16(sp)
	.cfi_restore 9
	ld	s2,8(sp)
	.cfi_restore 18
	ld	s3,0(sp)
	.cfi_restore 19
	lui	a0,%hi(.LC5)
	addi	a0,a0,%lo(.LC5)
	addi	sp,sp,32
	.cfi_def_cfa_offset 0
	tail	printf
	.cfi_endproc
.LFE977:
	.size	_Z4ssspv, .-_Z4ssspv
	.section	.rodata.str1.8
	.align	3
.LC6:
	.string	"ROI START "
	.align	3
.LC7:
	.string	"ROI END"
	.section	.text.startup,"ax",@progbits
	.align	1
	.globl	main
	.type	main, @function
main:
.LFB978:
	.cfi_startproc
	addi	sp,sp,-80
	.cfi_def_cfa_offset 80
	sd	s1,56(sp)
	li	a0,9
	.cfi_offset 9, -24
	mv	s1,a1
	li	a1,0
	sd	ra,72(sp)
	sd	s0,64(sp)
	.cfi_offset 1, -8
	.cfi_offset 8, -16
	call	atom_init
	ld	a1,8(s1)
	mv	a0,sp
	lui	s0,%hi(.LANCHOR0)
	call	_Z10read_graphPc
	ld	a5,0(sp)
	addi	s0,s0,%lo(.LANCHOR0)
	ld	a0,16(s1)
	sd	a5,0(s0)
	ld	a5,8(sp)
	addi	s1,s0,40
	sd	a5,8(s0)
	ld	a5,16(sp)
	sd	a5,16(s0)
	ld	a5,24(sp)
	sd	a5,24(s0)
	ld	a5,32(sp)
	sd	a5,32(s0)
	call	atoi
	mv	a4,a0
	lui	a0,%hi(.LC6)
	lui	a5,%hi(stride)
	addi	a0,a0,%lo(.LC6)
	sw	a4,%lo(stride)(a5)
	call	puts
	mv	a0,s1
	call	_ZN3HPC16startMeasurementEv
	lw	a5,28(s0)
	slli	a5,a5,2
	srli	a5,a5,9
	addiw	a5,a5,1
	slli	a5,a5,32
	srli	a5,a5,32
 #APP
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
 #NO_APP
	li	a3,0
	ld	a4,0(s0)
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map a4, a3 
	
# 0 "" 2
 #NO_APP
	lw	a4,32(s0)
	slli	a4,a4,2
	srli	a4,a4,9
	addiw	a4,a4,1
	slli	a4,a4,32
	srli	a4,a4,32
 #APP
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a4
	
# 0 "" 2
 #NO_APP
	ld	a2,8(s0)
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map a2, a3 
	
# 0 "" 2
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a4
	
# 0 "" 2
 #NO_APP
	ld	a4,16(s0)
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map a4, a3 
	
# 0 "" 2
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
 #NO_APP
	lui	a5,%hi(distance)
	ld	a5,%lo(distance)(a5)
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map a5, a3 
	
# 0 "" 2
 #NO_APP
	call	_Z4ssspv
	mv	a0,s1
	call	_ZN3HPC14endMeasurementEv
	lui	a0,%hi(.LC7)
	addi	a0,a0,%lo(.LC7)
	call	puts
	mv	a0,s1
	call	_ZN3HPC10printStatsEv
	mv	a0,s1
	call	_ZN3HPC8printCSVEv
	ld	ra,72(sp)
	.cfi_restore 1
	ld	s0,64(sp)
	.cfi_restore 8
	ld	s1,56(sp)
	.cfi_restore 9
	li	a0,0
	addi	sp,sp,80
	.cfi_def_cfa_offset 0
	jr	ra
	.cfi_endproc
.LFE978:
	.size	main, .-main
	.align	1
	.type	_GLOBAL__sub_I_distance, @function
_GLOBAL__sub_I_distance:
.LFB1303:
	.cfi_startproc
	addi	sp,sp,-16
	.cfi_def_cfa_offset 16
	sd	s0,0(sp)
	.cfi_offset 8, -16
	lui	s0,%hi(.LANCHOR0+40)
	addi	s0,s0,%lo(.LANCHOR0+40)
	mv	a0,s0
	sd	ra,8(sp)
	.cfi_offset 1, -8
	call	_ZN3HPCC1Ev
	mv	a1,s0
	ld	s0,0(sp)
	.cfi_restore 8
	ld	ra,8(sp)
	.cfi_restore 1
	lui	a2,%hi(__dso_handle)
	lui	a0,%hi(_ZN3HPCD1Ev)
	addi	a2,a2,%lo(__dso_handle)
	addi	a0,a0,%lo(_ZN3HPCD1Ev)
	addi	sp,sp,16
	.cfi_def_cfa_offset 0
	tail	__cxa_atexit
	.cfi_endproc
.LFE1303:
	.size	_GLOBAL__sub_I_distance, .-_GLOBAL__sub_I_distance
	.section	.init_array,"aw"
	.align	3
	.dword	_GLOBAL__sub_I_distance
	.globl	stride
	.globl	perfMon
	.globl	csr_graph
	.globl	distance
	.bss
	.align	3
	.set	.LANCHOR0,. + 0
	.type	csr_graph, @object
	.size	csr_graph, 40
csr_graph:
	.zero	40
	.type	perfMon, @object
	.size	perfMon, 440
perfMon:
	.zero	440
	.section	.sbss,"aw",@nobits
	.align	3
	.type	stride, @object
	.size	stride, 4
stride:
	.zero	4
	.zero	4
	.type	distance, @object
	.size	distance, 8
distance:
	.zero	8
	.hidden	__dso_handle
	.ident	"GCC: (g2ee5e430018) 12.2.0"
