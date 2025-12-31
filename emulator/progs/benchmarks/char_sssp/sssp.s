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
	sd	s1,88(sp)
	.cfi_offset 9, -24
	mv	s1,a0
	lui	a0,%hi(.LC0)
	addi	a0,a0,%lo(.LC0)
	sd	ra,104(sp)
	sd	s0,96(sp)
	sd	s2,80(sp)
	.cfi_offset 1, -8
	.cfi_offset 8, -16
	.cfi_offset 18, -32
	mv	s0,a1
	sd	s3,72(sp)
	sd	s4,64(sp)
	sd	s5,56(sp)
	sd	s6,48(sp)
	sd	s7,40(sp)
	sd	s8,32(sp)
	.cfi_offset 19, -40
	.cfi_offset 20, -48
	.cfi_offset 21, -56
	.cfi_offset 22, -64
	.cfi_offset 23, -72
	.cfi_offset 24, -80
	call	printf
	lui	a1,%hi(.LC1)
	addi	a1,a1,%lo(.LC1)
	mv	a0,s0
	call	fopen
	mv	s3,a0
	li	a0,1544192
	addi	a0,a0,-666
	call	srand
	lui	s7,%hi(.LC2)
	addi	a2,sp,12
	addi	a1,s7,%lo(.LC2)
	mv	a0,s3
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
	lw	s2,12(sp)
	lw	s8,16(sp)
	addiw	s4,s2,1
	slli	s4,s4,2
	mv	a0,s4
	slli	s5,s8,2
	sw	s2,28(s1)
	sw	s8,32(s1)
	call	malloc
	mv	s0,a0
	mv	a0,s5
	sd	s0,0(s1)
	call	malloc
	mv	s6,a0
	mv	a0,s5
	sd	s6,8(s1)
	call	malloc
	sd	a0,16(s1)
	sw	s2,24(s1)
	mv	s5,a0
	blt	s2,zero,.L2
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
	call	rand
	lw	a5,12(sp)
	lw	a4,16(sp)
	addi	s6,s6,4
	remw	a5,a0,a5
	addiw	s0,s0,1
	sw	a5,-4(s6)
	bgt	a4,s0,.L7
	ble	a4,zero,.L8
	li	s0,0
	lui	s7,%hi(.LC4)
	li	s6,5
.L9:
	addi	a2,sp,28
	addi	a1,s7,%lo(.LC4)
	mv	a0,s3
	call	fscanf
	call	rand
	remw	a5,a0,s6
	lw	a4,16(sp)
	addiw	s0,s0,1
	addi	s5,s5,4
	sw	a5,-4(s5)
	bgt	a4,s0,.L9
.L8:
	addi	a0,s4,-4
	call	malloc
	lui	a4,%hi(distance)
	sd	a0,%lo(distance)(a4)
	mv	a5,a0
	sext.w	a4,s2
	beq	s2,zero,.L1
.L5:
	slli	a3,a4,32
	srli	a4,a3,30
	li	a3,-2147483648
	add	a4,a5,a4
	xori	a3,a3,-1
.L10:
	sw	a3,0(a5)
	addi	a5,a5,4
	bne	a5,a4,.L10
.L1:
	ld	ra,104(sp)
	.cfi_remember_state
	.cfi_restore 1
	ld	s0,96(sp)
	.cfi_restore 8
	ld	s2,80(sp)
	.cfi_restore 18
	ld	s3,72(sp)
	.cfi_restore 19
	ld	s4,64(sp)
	.cfi_restore 20
	ld	s5,56(sp)
	.cfi_restore 21
	ld	s6,48(sp)
	.cfi_restore 22
	ld	s7,40(sp)
	.cfi_restore 23
	ld	s8,32(sp)
	.cfi_restore 24
	mv	a0,s1
	ld	s1,88(sp)
	.cfi_restore 9
	addi	sp,sp,112
	.cfi_def_cfa_offset 0
	jr	ra
.L2:
	.cfi_restore_state
	bgt	s8,zero,.L4
	addi	a0,s4,-4
	call	malloc
	lui	a4,%hi(distance)
	sd	a0,%lo(distance)(a4)
	mv	a5,a0
	sext.w	a4,s2
	j	.L5
	.cfi_endproc
.LFE976:
	.size	_Z10read_graphPc, .-_Z10read_graphPc
	.align	1
	.globl	_Z4ssspv
	.type	_Z4ssspv, @function
_Z4ssspv:
.LFB977:
	.cfi_startproc
	lui	t0,%hi(.LANCHOR0)
	addi	sp,sp,-32
	.cfi_def_cfa_offset 32
	addi	t0,t0,%lo(.LANCHOR0)
	lui	a5,%hi(distance)
	sd	s0,24(sp)
	sd	s1,16(sp)
	ld	t4,%lo(distance)(a5)
	ld	t2,0(t0)
	.cfi_offset 8, -8
	.cfi_offset 9, -16
	ld	s1,8(t0)
	ld	s0,16(t0)
	li	t3,-2147483648
	sd	s2,8(sp)
	.cfi_offset 18, -24
	xori	t3,t3,-1
	li	s2,100
.L20:
	lw	a4,28(t0)
	li	a5,0
	beq	a4,zero,.L29
.L21:
	slli	a3,a5,32
	srli	a4,a3,30
	add	a4,t4,a4
	sw	t3,0(a4)
	lw	a4,28(t0)
	addiw	a5,a5,1
	bgtu	a4,a5,.L21
.L29:
	sw	zero,0(t4)
	lw	a5,28(t0)
	li	t6,0
	beq	a5,zero,.L25
.L22:
	slli	a4,t6,32
	addiw	a5,t6,1
	srli	a7,a4,30
	slli	a4,a5,32
	srli	t5,a4,30
	add	t5,t2,t5
	add	a4,t2,a7
	lw	a4,0(a4)
	lw	a6,0(t5)
	sext.w	t6,a5
	bgeu	a4,a6,.L28
	slli	a3,a4,2
	add	a1,s1,a3
	add	a7,t4,a7
	add	a3,s0,a3
.L27:
	lw	a0,0(a7)
	addi	a4,a4,1
	beq	a0,t3,.L26
	lw	a5,0(a1)
	lw	a2,0(a3)
	slli	a5,a5,2
	add	a5,t4,a5
	lw	t1,0(a5)
	addw	a0,a2,a0
	ble	t1,a0,.L26
	sw	a0,0(a5)
	lw	a6,0(t5)
.L26:
	addi	a1,a1,4
	addi	a3,a3,4
	bgtu	a6,a4,.L27
.L28:
	lw	a5,28(t0)
	bgtu	a5,t6,.L22
.L25:
	addiw	s2,s2,-1
	bne	s2,zero,.L20
	ld	s0,24(sp)
	.cfi_restore 8
	ld	s1,16(sp)
	.cfi_restore 9
	ld	s2,8(sp)
	.cfi_restore 18
	addi	sp,sp,32
	.cfi_def_cfa_offset 0
	jr	ra
	.cfi_endproc
.LFE977:
	.size	_Z4ssspv, .-_Z4ssspv
	.section	.rodata.str1.8
	.align	3
.LC5:
	.string	"ROI START "
	.align	3
.LC6:
	.string	"ROI END"
	.section	.text.startup,"ax",@progbits
	.align	1
	.globl	main
	.type	main, @function
main:
.LFB978:
	.cfi_startproc
	addi	sp,sp,-464
	.cfi_def_cfa_offset 464
	sd	s0,448(sp)
	li	a0,9
	.cfi_offset 8, -16
	mv	s0,a1
	li	a1,0
	sd	ra,456(sp)
	sd	s1,440(sp)
	sd	s2,432(sp)
	.cfi_offset 1, -8
	.cfi_offset 9, -24
	.cfi_offset 18, -32
	call	atom_init
	ld	a1,8(s0)
	mv	a0,sp
	lui	s0,%hi(.LANCHOR0)
	call	_Z10read_graphPc
	ld	a5,0(sp)
	addi	s0,s0,%lo(.LANCHOR0)
	lui	a0,%hi(.LC5)
	sd	a5,0(s0)
	ld	a5,8(sp)
	li	s2,1
	addi	a0,a0,%lo(.LC5)
	sd	a5,8(s0)
	ld	a5,16(sp)
	addi	s1,s0,40
	sd	a5,16(s0)
	ld	a5,24(sp)
	sd	a5,24(s0)
	ld	a5,32(sp)
	sd	a5,32(s0)
	lui	a5,%hi(stride)
	sw	s2,%lo(stride)(a5)
	call	puts
	mv	a0,s1
	call	_ZN3HPC16startMeasurementEv
	li	a5,0
 #APP
# 136 "../../XMemLib/atomops.h" 1
	ast_deactivate a5 
	
# 0 "" 2
# 130 "../../XMemLib/atomops.h" 1
	ast_activate s2 
	
# 0 "" 2
 #NO_APP
	li	a5,2
 #APP
# 130 "../../XMemLib/atomops.h" 1
	ast_activate a5 
	
# 0 "" 2
 #NO_APP
	li	a5,3
 #APP
# 130 "../../XMemLib/atomops.h" 1
	ast_activate a5 
	
# 0 "" 2
 #NO_APP
	li	a5,1
	ld	t1,0(s0)
	lw	a1,28(s0)
	ld	a6,8(s0)
	lw	a2,32(s0)
	ld	a7,16(s0)
 #APP
# 92 "../../XMemLib/atomops.h" 1
	fatom_select a5, a5
	
# 0 "" 2
 #NO_APP
	li	a5,0
	addi	a4,sp,48
	li	a0,8
.L41:
	lw	a3,0(a4)
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a5, a3
	
# 0 "" 2
 #NO_APP
	addiw	a5,a5,1
	addi	a4,a4,4
	bne	a5,a0,.L41
	li	a5,2
 #APP
# 92 "../../XMemLib/atomops.h" 1
	fatom_select a5, a5
	
# 0 "" 2
 #NO_APP
	li	a5,0
	addi	a4,sp,176
	li	a0,8
.L42:
	lw	a3,0(a4)
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a5, a3
	
# 0 "" 2
 #NO_APP
	addiw	a5,a5,1
	addi	a4,a4,4
	bne	a5,a0,.L42
	li	a5,3
 #APP
# 92 "../../XMemLib/atomops.h" 1
	fatom_select a5, a5
	
# 0 "" 2
 #NO_APP
	li	a5,0
	addi	a4,sp,304
	li	a0,8
.L43:
	lw	a3,0(a4)
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a5, a3
	
# 0 "" 2
 #NO_APP
	addiw	a5,a5,1
	addi	a4,a4,4
	bne	a5,a0,.L43
	slli	a5,a1,2
	srli	a5,a5,9
	addiw	a5,a5,1
	slli	a5,a5,32
	srli	a5,a5,32
 #APP
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
 #NO_APP
	li	a5,1
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map t1, a5 
	
# 0 "" 2
 #NO_APP
	slli	a5,a2,2
	srli	a5,a5,9
	addiw	a5,a5,1
	slli	a5,a5,32
	srli	a5,a5,32
 #APP
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
 #NO_APP
	li	a4,2
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map a6, a4 
	
# 0 "" 2
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
 #NO_APP
	li	a5,3
 #APP
# 71 "../../XMemLib/atomops.h" 1
	amu_map a7, a5 
	
# 0 "" 2
 #NO_APP
	call	_Z4ssspv
	mv	a0,s1
	call	_ZN3HPC14endMeasurementEv
	lui	a0,%hi(.LC6)
	addi	a0,a0,%lo(.LC6)
	call	puts
	mv	a0,s1
	call	_ZN3HPC10printStatsEv
	mv	a0,s1
	call	_ZN3HPC8printCSVEv
	ld	ra,456(sp)
	.cfi_restore 1
	ld	s0,448(sp)
	.cfi_restore 8
	ld	s1,440(sp)
	.cfi_restore 9
	ld	s2,432(sp)
	.cfi_restore 18
	li	a0,0
	addi	sp,sp,464
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
