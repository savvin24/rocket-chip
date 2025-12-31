	.file	"tf.cpp"
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
.LFB1162:
	.cfi_startproc
	addi	sp,sp,-112
	.cfi_def_cfa_offset 112
	sd	s5,56(sp)
	.cfi_offset 21, -56
	mv	s5,a0
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
	sd	s4,64(sp)
	sd	s6,48(sp)
	sd	s7,40(sp)
	.cfi_offset 18, -32
	.cfi_offset 19, -40
	.cfi_offset 20, -48
	.cfi_offset 22, -64
	.cfi_offset 23, -72
	call	printf
	lui	a1,%hi(.LC1)
	addi	a1,a1,%lo(.LC1)
	mv	a0,s0
	call	fopen
	lui	s6,%hi(.LC2)
	addi	a2,sp,12
	addi	a1,s6,%lo(.LC2)
	mv	s4,a0
	call	fscanf
	addi	a2,sp,16
	addi	a1,s6,%lo(.LC2)
	mv	a0,s4
	call	fscanf
	lw	a2,16(sp)
	lw	a1,12(sp)
	lui	a0,%hi(.LC3)
	addi	a0,a0,%lo(.LC3)
	call	printf
	lw	s7,12(sp)
	lw	a5,16(sp)
	li	s1,0
	addiw	a0,s7,1
	slli	a0,a0,2
	slli	s3,a5,2
	sw	a5,32(s5)
	sw	s7,28(s5)
	call	malloc
	mv	s0,a0
	mv	a0,s3
	sd	s0,0(s5)
	call	malloc
	mv	s2,a0
	mv	a0,s3
	sd	s2,8(s5)
	call	malloc
	mv	s3,a0
	li	a0,1544192
	addi	a0,a0,-666
	sd	s3,16(s5)
	sw	s7,24(s5)
	call	srand
	lw	a5,12(sp)
	blt	a5,zero,.L6
.L5:
	addi	a2,sp,20
	addi	a1,s6,%lo(.LC2)
	mv	a0,s4
	call	fscanf
	lw	a4,20(sp)
	lw	a5,12(sp)
	addiw	s1,s1,1
	sw	a4,0(s0)
	addi	s0,s0,4
	bge	a5,s1,.L5
.L6:
	lw	a5,16(sp)
	li	s1,0
	ble	a5,zero,.L1
.L7:
	addi	a2,sp,24
	addi	a1,s6,%lo(.LC2)
	mv	a0,s4
	call	fscanf
	call	rand
	lw	a5,12(sp)
	lw	a4,16(sp)
	addi	s2,s2,4
	remw	a5,a0,a5
	addiw	s1,s1,1
	sw	a5,-4(s2)
	bgt	a4,s1,.L7
	ble	a4,zero,.L1
	li	s1,0
	lui	s2,%hi(.LC4)
.L8:
	addi	a2,sp,28
	addi	a1,s2,%lo(.LC4)
	mv	a0,s4
	call	fscanf
	flw	fa5,28(sp)
	lw	a5,16(sp)
	addi	s3,s3,4
	fcvt.w.s a4,fa5,rtz
	addiw	s1,s1,1
	sw	a4,-4(s3)
	bgt	a5,s1,.L8
.L1:
	ld	ra,104(sp)
	.cfi_restore 1
	ld	s0,96(sp)
	.cfi_restore 8
	ld	s1,88(sp)
	.cfi_restore 9
	ld	s2,80(sp)
	.cfi_restore 18
	ld	s3,72(sp)
	.cfi_restore 19
	ld	s4,64(sp)
	.cfi_restore 20
	ld	s6,48(sp)
	.cfi_restore 22
	ld	s7,40(sp)
	.cfi_restore 23
	mv	a0,s5
	ld	s5,56(sp)
	.cfi_restore 21
	addi	sp,sp,112
	.cfi_def_cfa_offset 0
	jr	ra
	.cfi_endproc
.LFE1162:
	.size	_Z10read_graphPc, .-_Z10read_graphPc
	.align	1
	.globl	_Z9benchmarkPiS_
	.type	_Z9benchmarkPiS_, @function
_Z9benchmarkPiS_:
.LFB1163:
	.cfi_startproc
	addi	sp,sp,-64
	.cfi_def_cfa_offset 64
	sd	s0,48(sp)
	.cfi_offset 8, -16
	lui	s0,%hi(.LANCHOR0)
	addi	s0,s0,%lo(.LANCHOR0)
	lw	a5,28(s0)
	sd	s1,40(sp)
	sd	s2,32(sp)
	sd	s3,24(sp)
	sd	s5,8(sp)
	sd	ra,56(sp)
	sd	s4,16(sp)
	.cfi_offset 9, -24
	.cfi_offset 18, -32
	.cfi_offset 19, -40
	.cfi_offset 21, -56
	.cfi_offset 1, -8
	.cfi_offset 20, -48
	mv	s1,a0
	mv	s5,a1
	li	s3,100
	li	s2,1
.L18:
	beq	a5,zero,.L17
	li	s4,0
.L19:
	call	rand
	srliw	a4,a0,31
	slli	a3,s4,32
	addw	a0,a4,a0
	srli	a5,a3,30
	andi	a0,a0,1
	add	a3,s1,a5
	subw	a0,a0,a4
	sw	a0,0(a3)
	add	a5,s5,a5
	sw	zero,0(a5)
	lw	a5,28(s0)
	addiw	s4,s4,1
	bgtu	a5,s4,.L19
	beq	a5,zero,.L17
	ld	a1,0(s0)
	ld	a7,8(s0)
	mv	a6,s1
	addi	a1,a1,4
	li	a0,0
	j	.L23
.L21:
	addi	a6,a6,4
	addi	a1,a1,4
	bleu	a5,a0,.L35
.L23:
	lw	a4,0(a6)
	addi	a0,a0,1
	bne	a4,s2,.L21
	lw	a3,-4(a1)
	lw	a4,0(a1)
	bgeu	a3,a4,.L21
	slli	a2,a3,2
	add	a2,a7,a2
.L22:
	lw	a5,0(a2)
	addi	a3,a3,1
	addi	a2,a2,4
	slli	a5,a5,2
	add	a5,s5,a5
	lw	a4,0(a5)
	addiw	a4,a4,1
	sw	a4,0(a5)
	lw	a5,0(a1)
	bgtu	a5,a3,.L22
	lw	a5,28(s0)
	addi	a6,a6,4
	addi	a1,a1,4
	bgtu	a5,a0,.L23
.L35:
	addiw	s3,s3,-1
	bne	s3,zero,.L18
.L17:
	ld	ra,56(sp)
	.cfi_restore 1
	ld	s0,48(sp)
	.cfi_restore 8
	ld	s1,40(sp)
	.cfi_restore 9
	ld	s2,32(sp)
	.cfi_restore 18
	ld	s3,24(sp)
	.cfi_restore 19
	ld	s4,16(sp)
	.cfi_restore 20
	ld	s5,8(sp)
	.cfi_restore 21
	addi	sp,sp,64
	.cfi_def_cfa_offset 0
	jr	ra
	.cfi_endproc
.LFE1163:
	.size	_Z9benchmarkPiS_, .-_Z9benchmarkPiS_
	.section	.rodata.str1.8
	.align	3
.LC5:
	.string	"Teenage Follower Finished"
	.section	.text.startup,"ax",@progbits
	.align	1
	.globl	main
	.type	main, @function
main:
.LFB1164:
	.cfi_startproc
	.cfi_personality 0x9b,DW.ref.__gxx_personality_v0
	.cfi_lsda 0x1b,.LLSDA1164
	addi	sp,sp,-592
	.cfi_def_cfa_offset 592
	sd	s0,576(sp)
	li	a0,9
	.cfi_offset 8, -16
	mv	s0,a1
	li	a1,0
	sd	ra,584(sp)
	sd	s1,568(sp)
	sd	s2,560(sp)
	sd	s6,528(sp)
	sd	s3,552(sp)
	sd	s4,544(sp)
	sd	s5,536(sp)
	sd	s7,520(sp)
	sd	s8,512(sp)
	sd	s9,504(sp)
	.cfi_offset 1, -8
	.cfi_offset 9, -24
	.cfi_offset 18, -32
	.cfi_offset 22, -64
	.cfi_offset 19, -40
	.cfi_offset 20, -48
	.cfi_offset 21, -56
	.cfi_offset 23, -72
	.cfi_offset 24, -80
	.cfi_offset 25, -88
.LEHB0:
	call	atom_init
	ld	a1,8(s0)
	mv	a0,sp
	lui	s2,%hi(.LANCHOR0)
	call	_Z10read_graphPc
	ld	a4,0(sp)
	addi	s2,s2,%lo(.LANCHOR0)
	ld	a5,24(sp)
	sd	a4,0(s2)
	ld	a4,8(sp)
	srai	s1,a5,32
	slli	s1,s1,2
	sd	a4,8(s2)
	ld	a4,16(sp)
	sd	a5,24(s2)
	li	a1,1
	sd	a4,16(s2)
	ld	a5,32(sp)
	mv	a0,s1
	lui	s6,%hi(stride)
	sd	a5,32(s2)
	li	a5,1
	sw	a5,%lo(stride)(s6)
	call	calloc
	li	a1,1
	mv	s0,a0
	mv	a0,s1
	call	calloc
	mv	s1,a0
	li	a0,1337
	call	srand
	addi	a0,sp,56
	call	_ZN3HPCC1Ev
.LEHE0:
	addi	a0,sp,56
.LEHB1:
	call	_ZN3HPC16startMeasurementEv
	li	a5,0
 #APP
# 136 "../../XMemLib/atomops.h" 1
	ast_deactivate a5 
	
# 0 "" 2
 #NO_APP
	li	s4,1
 #APP
# 130 "../../XMemLib/atomops.h" 1
	ast_activate s4 
	
# 0 "" 2
 #NO_APP
	li	ra,2
 #APP
# 130 "../../XMemLib/atomops.h" 1
	ast_activate ra 
	
# 0 "" 2
 #NO_APP
	li	t2,3
 #APP
# 130 "../../XMemLib/atomops.h" 1
	ast_activate t2 
	
# 0 "" 2
 #NO_APP
	li	s5,4
 #APP
# 130 "../../XMemLib/atomops.h" 1
	ast_activate s5 
	
# 0 "" 2
 #NO_APP
	li	t3,1
	ld	s3,0(s2)
	lw	a4,28(s2)
	ld	t0,16(s2)
	lw	a5,32(s2)
	lw	a2,%lo(stride)(s6)
	ld	s2,8(s2)
 #APP
# 92 "../../XMemLib/atomops.h" 1
	fatom_select t3, t3
	
# 0 "" 2
 #NO_APP
	li	a3,0
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a3, s3
	
# 0 "" 2
 #NO_APP
	srli	a1,s3,32
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t3, a1
	
# 0 "" 2
 #NO_APP
	li	t1,2
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t1, a3
	
# 0 "" 2
 #NO_APP
	li	a7,3
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a7, a3
	
# 0 "" 2
 #NO_APP
	li	a6,4
	sext.w	s8,a4
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a6, s8
	
# 0 "" 2
 #NO_APP
	li	s6,5
	srai	s7,a4,63
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load s6, s7
	
# 0 "" 2
 #NO_APP
	li	t5,6
	sext.w	t6,a2
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t5, t6
	
# 0 "" 2
 #NO_APP
	li	t4,7
	srai	a2,a2,63
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t4, a2
	
# 0 "" 2
# 92 "../../XMemLib/atomops.h" 1
	fatom_select t1, t1
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a3, t0
	
# 0 "" 2
 #NO_APP
	srli	a1,t0,32
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t3, a1
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t1, a3
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a7, a3
	
# 0 "" 2
 #NO_APP
	sext.w	a0,a5
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a6, a0
	
# 0 "" 2
 #NO_APP
	srai	a1,a5,63
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load s6, a1
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t5, t6
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t4, a2
	
# 0 "" 2
# 92 "../../XMemLib/atomops.h" 1
	fatom_select a7, a7
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a3, s2
	
# 0 "" 2
 #NO_APP
	srli	s9,s2,32
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t3, s9
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t1, s1
	
# 0 "" 2
 #NO_APP
	srli	s9,s1,32
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a7, s9
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a6, a0
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load s6, a1
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t5, t6
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t4, a2
	
# 0 "" 2
# 92 "../../XMemLib/atomops.h" 1
	fatom_select a6, a6
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a3, s0
	
# 0 "" 2
 #NO_APP
	srli	a1,s0,32
 #APP
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t3, a1
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t1, a3
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a7, a3
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load a6, s8
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load s6, s7
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t5, t6
	
# 0 "" 2
# 98 "../../XMemLib/atomops.h" 1
	fatom_load t4, a2
	
# 0 "" 2
 #NO_APP
	slli	a4,a4,2
	srli	a4,a4,9
	addiw	a4,a4,1
	slli	a4,a4,32
	srli	a4,a4,32
 #APP
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a4
	
# 0 "" 2
# 71 "../../XMemLib/atomops.h" 1
	amu_map s0, s5 
	
# 0 "" 2
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a4
	
# 0 "" 2
# 71 "../../XMemLib/atomops.h" 1
	amu_map s3, s4 
	
# 0 "" 2
 #NO_APP
	slli	a5,a5,2
	srli	a5,a5,9
	addiw	a5,a5,1
	slli	a5,a5,32
	srli	a5,a5,32
 #APP
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
# 71 "../../XMemLib/atomops.h" 1
	amu_map t0, ra 
	
# 0 "" 2
# 29 "../../XMemLib/atomops.h" 1
	amu_write_len a5
	
# 0 "" 2
# 71 "../../XMemLib/atomops.h" 1
	amu_map s2, t2 
	
# 0 "" 2
 #NO_APP
	mv	a0,s0
	mv	a1,s1
	call	_Z9benchmarkPiS_
	addi	a0,sp,56
	call	_ZN3HPC14endMeasurementEv
	lui	a0,%hi(.LC5)
	addi	a0,a0,%lo(.LC5)
	call	puts
	addi	a0,sp,56
	call	_ZN3HPC10printStatsEv
	addi	a0,sp,56
	call	_ZN3HPC8printCSVEv
.LEHE1:
	addi	a0,sp,56
	call	_ZN3HPCD1Ev
	ld	ra,584(sp)
	.cfi_remember_state
	.cfi_restore 1
	ld	s0,576(sp)
	.cfi_restore 8
	ld	s1,568(sp)
	.cfi_restore 9
	ld	s2,560(sp)
	.cfi_restore 18
	ld	s3,552(sp)
	.cfi_restore 19
	ld	s4,544(sp)
	.cfi_restore 20
	ld	s5,536(sp)
	.cfi_restore 21
	ld	s6,528(sp)
	.cfi_restore 22
	ld	s7,520(sp)
	.cfi_restore 23
	ld	s8,512(sp)
	.cfi_restore 24
	ld	s9,504(sp)
	.cfi_restore 25
	li	a0,0
	addi	sp,sp,592
	.cfi_def_cfa_offset 0
	jr	ra
.L38:
	.cfi_restore_state
	mv	s0,a0
	addi	a0,sp,56
	call	_ZN3HPCD1Ev
	mv	a0,s0
.LEHB2:
	call	_Unwind_Resume
.LEHE2:
	.cfi_endproc
.LFE1164:
	.globl	__gxx_personality_v0
	.section	.gcc_except_table,"aw",@progbits
.LLSDA1164:
	.byte	0xff
	.byte	0xff
	.byte	0x3
	.byte	0x27
	.4byte	.LEHB0-.LFB1164
	.4byte	.LEHE0-.LEHB0
	.4byte	0
	.byte	0
	.4byte	.LEHB1-.LFB1164
	.4byte	.LEHE1-.LEHB1
	.4byte	.L38-.LFB1164
	.byte	0
	.4byte	.LEHB2-.LFB1164
	.4byte	.LEHE2-.LEHB2
	.4byte	0
	.byte	0
	.section	.text.startup
	.size	main, .-main
	.globl	stride
	.globl	csr_graph
	.bss
	.align	3
	.set	.LANCHOR0,. + 0
	.type	csr_graph, @object
	.size	csr_graph, 40
csr_graph:
	.zero	40
	.section	.sbss,"aw",@nobits
	.align	2
	.type	stride, @object
	.size	stride, 4
stride:
	.zero	4
	.hidden	DW.ref.__gxx_personality_v0
	.weak	DW.ref.__gxx_personality_v0
	.section	.sdata.DW.ref.__gxx_personality_v0,"awG",@progbits,DW.ref.__gxx_personality_v0,comdat
	.align	3
	.type	DW.ref.__gxx_personality_v0, @object
	.size	DW.ref.__gxx_personality_v0, 8
DW.ref.__gxx_personality_v0:
	.dword	__gxx_personality_v0
	.ident	"GCC: (g2ee5e430018) 12.2.0"
