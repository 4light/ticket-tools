;(window.webpackJsonp = window.webpackJsonp || []).push([
    [14],
    {
        713: function (e, t) {},
        717: function (e, t, r) {
            var content = r(945)
            content.__esModule && (content = content.default),
            'string' == typeof content && (content = [[e.i, content, '']]),
            content.locals && (e.exports = content.locals)
            ;(0, r(15).default)('06753a59', content, !0, { sourceMap: !1 })
        },
        910: function (e, t, r) {
            e.exports = r.p + 'img/20210807125850.c9df981.png'
        },
        916: function (e, t) {},
        917: function (e, t) {},
        944: function (e, t, r) {
            'use strict'
            r(717)
        },
        945: function (e, t, r) {
            var n = r(14)(!1)
            n.push([
                e.i,
                '.btns[data-v-e59228b0]{display:flex;align-items:center}.maintitle[data-v-e59228b0],.tips-wrap[data-v-e59228b0]{display:flex;justify-content:space-between;align-items:center}.maintitle[data-v-e59228b0]{margin-bottom:20px}.td-idnum-inner[data-v-e59228b0]{display:flex;flex-direction:column;align-items:center;margin-bottom:6px}.td-idnum-inner .idnumwrap[data-v-e59228b0]{display:flex;justify-content:center;align-items:center;position:relative}.td-idnum-inner .idnumwrap .jkb[data-v-e59228b0]{position:absolute;right:-10px;transform:translateX(100%);color:#fff;background:#49c660;border-radius:6px;padding:5px 8px;margin-left:12px;display:flex;align-items:center;-webkit-user-select:none;-moz-user-select:none;user-select:none;cursor:pointer;font-size:12px}.td-idnum-inner .idnumwrap .jkb i[data-v-e59228b0]{font-weight:700;font-size:12px;margin-right:4px}.jkb-dialog-body[data-v-e59228b0]{text-align:center}.jkb-dialog-body .p1[data-v-e59228b0]{font-weight:700;font-size:18px;color:#333;margin:12px 0 20px}.jkb-dialog-body img[data-v-e59228b0]{width:200px}.jkb-dialog-body .p2[data-v-e59228b0]{margin:16px 0}.adduser-dialog-body[data-v-e59228b0]{display:flex;justify-content:space-evenly}.adduser-dialog-body .jkb-qrcode[data-v-e59228b0]{text-align:center;width:200px;font-weight:700;color:#333}.adduser-dialog-body .jkb-qrcode img[data-v-e59228b0]{display:block;width:150px;margin:0 auto 10px}',
                '',
            ]),
                (e.exports = n)
        },
        981: function (e, t, r) {
            'use strict'
            r.r(t)
            r(180), r(12), r(20), r(60), r(649), r(34), r(25), r(51), r(33), r(52)
            var n = r(17),
                o = r(6),
                d =
                    (r(28),
                        r(109),
                        r(54),
                        r(676),
                        r(29),
                        r(35),
                        r(44),
                        r(77),
                        r(53),
                        r(245),
                        r(68),
                        r(911)),
                c = r.n(d),
                l = r(79)
            function m(object, e) {
                var t = Object.keys(object)
                if (Object.getOwnPropertySymbols) {
                    var r = Object.getOwnPropertySymbols(object)
                    e &&
                    (r = r.filter(function (e) {
                        return Object.getOwnPropertyDescriptor(object, e).enumerable
                    })),
                        t.push.apply(t, r)
                }
                return t
            }
            function v(e) {
                for (var i = 1; i < arguments.length; i++) {
                    var source = null != arguments[i] ? arguments[i] : {}
                    i % 2
                        ? m(Object(source), !0).forEach(function (t) {
                            Object(n.a)(e, t, source[t])
                        })
                        : Object.getOwnPropertyDescriptors
                        ? Object.defineProperties(
                            e,
                            Object.getOwnPropertyDescriptors(source)
                        )
                        : m(Object(source)).forEach(function (t) {
                            Object.defineProperty(
                                e,
                                t,
                                Object.getOwnPropertyDescriptor(source, t)
                            )
                        })
                }
                return e
            }
            var h = {
                    components: { Verify: r(854).default },
                    asyncData: function (e) {
                        var t = e.params,
                            r = e.query
                        return {
                            captchaType: '',
                            isDev: !1,
                            showJkbDialog: !1,
                            showGuidesDialog: !1,
                            guides: null,
                            tglist: [],
                            loading: !1,
                            type: t.type,
                            eventssessionid: t.eventssessionid,
                            booking_including_self: +r.booking_including_self,
                            maxnums: r.maxnums,
                            minnums: r.minnums,
                            confirmOrder: !1,
                            dialogShow: !1,
                            selectedUser: null,
                            userList: [],
                            doctypeOptions: [
                                { label: '身份证', value: 'IDCARD' },
                                { label: '护照', value: 'PASSPORT' },
                                { label: '港澳居民来往内地通行证', value: 'HMCARD' },
                                { label: '台湾居民来往大陆通行证', value: 'TCARD' },
                            ],
                            signForm: {},
                        }
                    },
                    data: function () {
                        var e,
                            t = this
                        return {
                            rules: {
                                realname: [
                                    {
                                        required: !0,
                                        message: this.$t(
                                            'JNT.createOrder.addUserForm.realname.msgs[0]'
                                        ),
                                        trigger: 'blur',
                                    },
                                    {
                                        trigger: 'blur',
                                        validator: function (e, r, n, source, o) {
                                            t.validatorRealname(r, t.signForm.doctype, n)
                                        },
                                    },
                                ],
                                doctype: [
                                    {
                                        required: !0,
                                        message: this.$t(
                                            'JNT.createOrder.addUserForm.doctype.msgs[0]'
                                        ),
                                        trigger: 'blur',
                                    },
                                ],
                                idnum: [
                                    {
                                        required: !0,
                                        message: this.$t(
                                            'JNT.createOrder.addUserForm.idnum.msgs[8]'
                                        ),
                                        trigger: 'blur',
                                    },
                                    {
                                        trigger: 'blur',
                                        validator:
                                            ((e = Object(o.a)(
                                                regeneratorRuntime.mark(function e(r, n, o, source, d) {
                                                    return regeneratorRuntime.wrap(function (e) {
                                                        for (;;)
                                                            switch ((e.prev = e.next)) {
                                                                case 0:
                                                                    return (
                                                                        (e.next = 2),
                                                                            t.idnumValidator(
                                                                                n,
                                                                                t.signForm.doctype,
                                                                                o,
                                                                                t.selectedUser
                                                                            )
                                                                    )
                                                                case 2:
                                                                case 'end':
                                                                    return e.stop()
                                                            }
                                                    }, e)
                                                })
                                            )),
                                                function (t, r, n, o, d) {
                                                    return e.apply(this, arguments)
                                                }),
                                    },
                                ],
                            },
                        }
                    },
                    computed: v(
                        v({}, Object(l.c)(['jkb'])),
                        {},
                        {
                            accountType: function () {
                                return this.$store.state.accountType
                            },
                            userInfo: function () {
                                return this.$store.state.userInfo || {}
                            },
                        }
                    ),
                    mounted: function () {
                        var e = this
                        if (
                            ('ta' === this.type &&
                            this.$axios
                                .$post('/ajax?ugi=ta/relation&action=tglist', {
                                    eventssessionid: this.eventssessionid,
                                })
                                .then(function (t) {
                                    'A00006' === t.code &&
                                    (e.tglist = t.tourguides.filter(function (e) {
                                        return (
                                            (e.isConflict = t.conflicts.some(function (t) {
                                                return t === e.guideid
                                            })),
                                            2 === e.joinstatus
                                        )
                                    }))
                                }),
                            'user' === this.type && 1 === this.booking_including_self)
                        ) {
                            var t = v(
                                v(
                                    {},
                                    this.createUserObj(
                                        this.userInfo.realname,
                                        this.userInfo.idtype,
                                        this.userInfo.idnum
                                    )
                                ),
                                {},
                                { isSelf: !0 }
                            )
                            this.checkBookingUsers([t]), this.userList.push(t)
                        }
                    },
                    methods: {
                        success: function () {
                            console.log('1212'),
                                (this.showverify = !1),
                                this.handleConfirmOrder()
                        },
                        useVerify: function () {
                            this.$refs.verify.show()
                        },
                        checkBookingUsers: function (e) {
                            this.$axios
                                .$post('/ajax?ugi=bookingorder&action=checkBookingUserV2', {
                                    eventssessionid: this.eventssessionid,
                                    usertype: this.type,
                                    idnums: e
                                        .map(function (e) {
                                            return e.idnum
                                        })
                                        .join(','),
                                })
                                .then(function (t) {
                                    if ('A00006' === t.code) {
                                        if (t.issues) {
                                            var r = function (r) {
                                                if (Object.hasOwnProperty.call(t.issues, r)) {
                                                    var n = t.issues[r],
                                                        o = e.find(function (e) {
                                                            return e.idnum === r
                                                        })
                                                    o &&
                                                    (1 === t.jkb && (o.jkbColor = n.color),
                                                        n.errmsg
                                                            ? ((o.invalid = !0), (o.errmsg_idnum = n.errmsg))
                                                            : 1 === t.jkb &&
                                                            Object.hasOwnProperty.call(n, 'color') &&
                                                            0 !== n.color &&
                                                            ((o.invalid = !0),
                                                                (o.errmsg_idnum = n.colormsg)))
                                                }
                                            }
                                            for (var n in t.issues) r(n)
                                        }
                                    } else
                                        e.map(function (e) {
                                            ;(e.invalid = !0), (e.errmsg_idnum = '校验失败，请重试')
                                        })
                                })
                        },
                        validatorRealname: function (e, t, r) {
                            var n = this
                            return Object(o.a)(
                                regeneratorRuntime.mark(function o() {
                                    return regeneratorRuntime.wrap(function (o) {
                                        for (;;)
                                            switch ((o.prev = o.next)) {
                                                case 0:
                                                    if (
                                                        'IDCARD' !== t ||
                                                        /^[\u4E00-\u9FA5·^\s]+$/g.test(e)
                                                    ) {
                                                        o.next = 4
                                                        break
                                                    }
                                                    r(
                                                        new Error(
                                                            n.$t(
                                                                'JNT.createOrder.addUserForm.realname.msgs[1]'
                                                            )
                                                        )
                                                    ),
                                                        (o.next = 10)
                                                    break
                                                case 4:
                                                    if (
                                                        'PASSPORT' !== t ||
                                                        /^([\u4E00-\u9FA5·^\s]+|[a-zA-Z·^\s]+)$/gi.test(e)
                                                    ) {
                                                        o.next = 8
                                                        break
                                                    }
                                                    r(
                                                        new Error(
                                                            n.$t(
                                                                'JNT.createOrder.addUserForm.realname.msgs[2]'
                                                            )
                                                        )
                                                    ),
                                                        (o.next = 10)
                                                    break
                                                case 8:
                                                    return (
                                                        (o.next = 10),
                                                            n.$axios
                                                                .$post(
                                                                    '/ajax?ugi=account&action=checkSensitiveWord',
                                                                    { str: e }
                                                                )
                                                                .then(function (e) {
                                                                    'A00006' === e.code
                                                                        ? r()
                                                                        : r(new Error(e.errmsg))
                                                                })
                                                    )
                                                case 10:
                                                case 'end':
                                                    return o.stop()
                                            }
                                    }, o)
                                })
                            )()
                        },
                        idnumValidator: function (e, t, r, n, d) {
                            var c = this
                            return Object(o.a)(
                                regeneratorRuntime.mark(function o() {
                                    var l, m
                                    return regeneratorRuntime.wrap(function (o) {
                                        for (;;)
                                            switch ((o.prev = o.next)) {
                                                case 0:
                                                    if (
                                                        'IDCARD' !== t ||
                                                        /^[1-9]\d{5}(18|19|20)\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\d{3}[0-9Xx]$/.test(
                                                            e
                                                        )
                                                    ) {
                                                        o.next = 4
                                                        break
                                                    }
                                                    r(
                                                        new Error(
                                                            c.$t('JNT.createOrder.addUserForm.idnum.msgs[0]')
                                                        )
                                                    ),
                                                        (o.next = 32)
                                                    break
                                                case 4:
                                                    if ('IDCARD' !== t || c.$util.idCardValidator(e)) {
                                                        o.next = 8
                                                        break
                                                    }
                                                    r(
                                                        Error(
                                                            c.$t('JNT.createOrder.addUserForm.idnum.msgs[1]')
                                                        )
                                                    ),
                                                        (o.next = 32)
                                                    break
                                                case 8:
                                                    if (
                                                        'PASSPORT' !== t ||
                                                        /^([a-zA-z]|[0-9]){5,17}$/.test(e)
                                                    ) {
                                                        o.next = 12
                                                        break
                                                    }
                                                    r(
                                                        new Error(
                                                            c.$t('JNT.createOrder.addUserForm.idnum.msgs[2]')
                                                        )
                                                    ),
                                                        (o.next = 32)
                                                    break
                                                case 12:
                                                    if (
                                                        'HMCARD' !== t ||
                                                        /^([A-Z]\d{6,10}(\(\w{1}\))?)$/.test(e)
                                                    ) {
                                                        o.next = 16
                                                        break
                                                    }
                                                    r(
                                                        new Error(
                                                            c.$t('JNT.createOrder.addUserForm.idnum.msgs[4]')
                                                        )
                                                    ),
                                                        (o.next = 32)
                                                    break
                                                case 16:
                                                    if (
                                                        'TCARD' !== t ||
                                                        /^\d{8}|^[a-zA-Z0-9]{10}|^\d{18}$/.test(e)
                                                    ) {
                                                        o.next = 20
                                                        break
                                                    }
                                                    r(
                                                        new Error(
                                                            c.$t('JNT.createOrder.addUserForm.idnum.msgs[5]')
                                                        )
                                                    ),
                                                        (o.next = 32)
                                                    break
                                                case 20:
                                                    if (
                                                        !(
                                                            c.userList.findIndex(function (t) {
                                                                return t !== n && t.idnum === e
                                                            }) > -1
                                                        )
                                                    ) {
                                                        o.next = 24
                                                        break
                                                    }
                                                    r(
                                                        new Error(
                                                            c.$t('JNT.createOrder.addUserForm.idnum.msgs[6]')
                                                        )
                                                    ),
                                                        (o.next = 32)
                                                    break
                                                case 24:
                                                    if (d) {
                                                        o.next = 31
                                                        break
                                                    }
                                                    return (
                                                        (o.next = 27),
                                                            c.$axios.$post(
                                                                '/ajax?ugi=bookingorder&action=checkBookingUserV2',
                                                                {
                                                                    eventssessionid: c.eventssessionid,
                                                                    usertype: c.type,
                                                                    idnums: e,
                                                                }
                                                            )
                                                    )
                                                case 27:
                                                    'A00006' === (l = o.sent).code
                                                        ? l.issues
                                                        ? (m = l.issues[e]).errmsg
                                                            ? r(new Error(m.errmsg))
                                                            : 1 === l.jkb &&
                                                            Object.hasOwnProperty.call(m, 'color') &&
                                                            0 !== m.color
                                                                ? r(new Error(m.colormsg))
                                                                : (r(),
                                                                    (c.signForm.invalid = !1),
                                                                    (c.signForm.jkbColor =
                                                                        1 === l.jkb ? m.color : -2))
                                                        : r()
                                                        : r(new Error('网络错误')),
                                                        (o.next = 32)
                                                    break
                                                case 31:
                                                    r()
                                                case 32:
                                                case 'end':
                                                    return o.stop()
                                            }
                                    }, o)
                                })
                            )()
                        },
                        delAll: function () {
                            var e = this
                            this.$confirm(
                                this.$t('JNT.createOrder.delAll.msg'),
                                this.$t('JNT.createOrder.delAll.title'),
                                {
                                    confirmButtonText: this.$t(
                                        'JNT.createOrder.delAll.confirmButtonText'
                                    ),
                                    cancelButtonText: this.$t(
                                        'JNT.createOrder.delAll.cancelButtonText'
                                    ),
                                    type: 'warning',
                                }
                            )
                                .then(function () {
                                    var t = e.userList.find(function (e) {
                                        return e.isSelf
                                    })
                                    e.userList = t ? [t] : []
                                })
                                .catch(function () {})
                        },
                        handleUpload: function (e) {
                            return this.readExcel(e), !1
                        },
                        readExcel: function (e) {
                            var t = this
                            if (!e) return !1
                            if (!/\.(xls|xlsx|txt)$/.test(e.name.toLowerCase()))
                                return (
                                    this.$message({
                                        message: this.$t('JNT.createOrder.importMsg'),
                                        type: 'warning',
                                    }),
                                        !1
                                )
                            var r = new FileReader()
                            e.name.includes('.txt')
                                ? ((r.onload = function (e) {
                                    var data = e.target.result
                                    t.loadTxtFile(data)
                                }),
                                    r.readAsText(e))
                                : ((r.onload = function (e) {
                                    try {
                                        var data = e.target.result,
                                            r = c.a.read(data, { type: 'binary' }),
                                            n = r.SheetNames[0],
                                            o = c.a.utils.sheet_to_json(r.Sheets[n])
                                        t.checkXlsxJson(o)
                                    } catch (e) {
                                        return !1
                                    }
                                }),
                                    r.readAsBinaryString(e))
                        },
                        downloadxlsx: function () {
                            this.$util.open(
                                '/page/表格导入模板.xlsx',
                                this.$t('JNT.createOrder.xlsxFileName')
                            )
                        },
                        downloadtxt: function () {
                            this.$util.open(
                                '/page/文本导入模板.txt',
                                this.$t('JNT.createOrder.txtFileName')
                            )
                        },
                        onpaste: function (e) {
                            var t = this
                            setTimeout(function () {
                                var r = e.target.value
                                    .replace(/，/g, ',')
                                    .replace(/；/g, ';')
                                    .split(';')[0]
                                    .split(',')
                                r.length >= 2 &&
                                ((t.signForm.realname = r[0].trim()),
                                    (t.signForm.idnum = r[1].trim()),
                                    t.$refs.signForm.validateField('idnum'))
                            }, 200)
                        },
                        loadTxtFile: function (e) {
                            for (
                                var t = [],
                                    r = e.replace(/，/g, ',').replace(/；/g, ';').split(';'),
                                    i = 0;
                                i < r.length;
                                i++
                            ) {
                                var n = r[i].split(',')
                                if (n.length >= 2) {
                                    var o = n[0].trim(),
                                        d = n[1].trim()
                                    o && d && t.push(this.createUserObj(o, 'IDCARD', d))
                                }
                            }
                            this.checkList(t)
                        },
                        checkXlsxJson: function (e) {
                            var t = this
                            console.log('表格数据', e)
                            var r = []
                            try {
                                for (
                                    var n = function (i) {
                                            var n = e[i],
                                                o = t.doctypeOptions.find(function (option) {
                                                    return (
                                                        n['证件类型'] === option.label ||
                                                        n['证件类型'] === option.label_zh
                                                    )
                                                })
                                            r.push(
                                                t.createUserObj(
                                                    (n['姓名'] || '').trim(),
                                                    (o && o.value) || '',
                                                    (n['证件号码'] || '').trim()
                                                )
                                            )
                                        },
                                        i = 0;
                                    i < e.length;
                                    i++
                                )
                                    n(i)
                            } catch (e) {
                                console.log(e)
                            }
                            this.checkList(r)
                        },
                        createUserObj: function () {
                            var e =
                                    arguments.length > 0 && void 0 !== arguments[0]
                                        ? arguments[0]
                                        : '',
                                t =
                                    arguments.length > 1 && void 0 !== arguments[1]
                                        ? arguments[1]
                                        : 'IDCARD',
                                r =
                                    arguments.length > 2 && void 0 !== arguments[2]
                                        ? arguments[2]
                                        : ''
                            return {
                                invalid: !1,
                                errmsg_realname: '',
                                errmsg_doctype: '',
                                errmsg_idnum: '',
                                jkbColor: -2,
                                id: this.$util.guide(),
                                realname: e,
                                doctype: t,
                                idnum: r,
                            }
                        },
                        checkList: function (e) {
                            var t = this
                            this.userList = this.userList.concat(e)
                            for (
                                var r = function (i) {
                                        var r = e[i]
                                        r.doctype ||
                                        ((r.invalid = !0),
                                            (r.errmsg_doctype = t.$t('JNT.createOrder.importMsg1'))),
                                            t.validatorRealname(r.realname, r.doctype, function (e) {
                                                e && ((r.invalid = !0), (r.errmsg_realname = e.message))
                                            }),
                                            t.idnumValidator(
                                                r.idnum,
                                                r.doctype,
                                                function (e) {
                                                    e && ((r.invalid = !0), (r.errmsg_idnum = e.message))
                                                },
                                                r,
                                                !0
                                            )
                                    },
                                    i = 0;
                                i < e.length;
                                i++
                            )
                                r(i)
                            var n = this.userList.filter(function (e) {
                                return !e.invalid
                            })
                            n.length && this.checkBookingUsers(n)
                        },
                        addGuides: function () {
                            this.showGuidesDialog = !0
                        },
                        handleConfirmAddGuides: function (e) {
                            ;(this.guides = e), (this.showGuidesDialog = !1)
                        },
                        addUser: function () {
                            this.isDev
                                ? (this.signForm = this.createUserObj(
                                '孙山杉',
                                'IDCARD',
                                '420583199508130015'
                                ))
                                : (this.signForm = this.createUserObj('', 'IDCARD', '')),
                                (this.selectedUser = null),
                                (this.dialogShow = !0)
                        },
                        editUser: function (e) {
                            var t = this
                            ;(this.selectedUser = e),
                                (this.signForm = JSON.parse(JSON.stringify(e))),
                                (this.dialogShow = !0),
                                this.$nextTick(function () {
                                    t.$refs.signForm.validate()
                                })
                        },
                        removeUser: function (e) {
                            var t = this
                            ;(this.selectedUser = this.userList[e]),
                                this.$confirm(
                                    this.$t('JNT.createOrder.delOne.msg'),
                                    this.$t('JNT.createOrder.delOne.title'),
                                    {
                                        confirmButtonText: this.$t(
                                            'JNT.createOrder.delOne.confirmButtonText'
                                        ),
                                        cancelButtonText: this.$t(
                                            'JNT.createOrder.delOne.cancelButtonText'
                                        ),
                                        type: 'warning',
                                    }
                                ).then(function () {
                                    t.userList.splice(e, 1), t.fixRepeat()
                                })
                        },
                        fixRepeat: function () {
                            var e = this,
                                t = this.userList.filter(function (t) {
                                    return (
                                        t !== e.selectedUser && t.idnum === e.selectedUser.idnum
                                    )
                                })
                            1 === t.length &&
                            ((t[0].errmsg_idnum = ''),
                            t[0].errmsg_realname ||
                            t[0].errmsg_doctype ||
                            (t[0].invalid = !1))
                        },
                        handleAdd: function () {
                            var e = this
                            this.$refs.signForm.validate(function (t) {
                                if (!t) return !1
                                e.dialogShow = !1
                                try {
                                    e.selectedUser
                                        ? (e.fixRepeat(), Object.assign(e.selectedUser, e.signForm))
                                        : e.userList.push(
                                        JSON.parse(JSON.stringify(v({}, e.signForm)))
                                        )
                                } catch (e) {}
                                e.signForm = e.createUserObj('', 'IDCARD', '')
                            })
                        },
                        handleSubmit: function () {
                            this.userList.length
                                ? this.minnums > 0 && this.userList.length < this.minnums
                                ? this.$message.error(
                                    this.$t('JNT.createOrder.submitMsgs[1]', {
                                        minnums: this.minnums,
                                    })
                                )
                                : this.maxnums > 0 && this.userList.length > this.maxnums
                                    ? this.$message.error(
                                        this.$t('JNT.createOrder.submitMsgs[2]', {
                                            maxnums: this.maxnums,
                                        })
                                    )
                                    : this.userList.findIndex(function (e) {
                                        return -1 === e.jkbColor
                                    }) > -1
                                        ? (this.showJkbDialog = !0)
                                        : this.userList.findIndex(function (e) {
                                            return 0 !== e.jkbColor && -2 !== e.jkbColor && e.invalid
                                        }) > -1
                                            ? this.$message.error('健康码异常')
                                            : this.userList.findIndex(function (e) {
                                                return e.invalid
                                            }) > -1
                                                ? this.$message.error('瞻仰人信息有误，请修改后提交')
                                                : 'ta' !== this.type || this.guides
                                                    ? (this.confirmOrder = !0)
                                                    : this.$message.warning(
                                                        this.$t('JNT.createOrder.submitMsgs[4]')
                                                    )
                                : this.$message.error(this.$t('JNT.createOrder.submitMsgs[0]'))
                        },
                        handleConfirmOrder: function () {
                            var e = this
                            ;(this.loading = !0),
                                'user' === this.type
                                    ? this.$axios
                                        .$post(
                                            '/ajax?ugi=bookingorder&action=createTicketOrder',
                                            {
                                                eventssessionid: this.$route.params.eventssessionid,
                                                bookingdata: JSON.stringify(
                                                    this.userList.map(function (e) {
                                                        return {
                                                            realname: e.realname,
                                                            doctype: e.doctype,
                                                            idnum: e.idnum,
                                                        }
                                                    })
                                                ),
                                            }
                                        )
                                        .then(function (t) {
                                            ;(e.loading = !1),
                                                'A00006' === t.code
                                                    ? e.$router.push({
                                                        name: 'type-success-orderid',
                                                        params: {
                                                            type: e.type,
                                                            orderid: t.ticketorder.orderid,
                                                        },
                                                    })
                                                    : 'A00013' === t.code &&
                                                    ((e.captchaType = t.captcha_type),
                                                        setTimeout(function () {
                                                            e.useVerify()
                                                        }, 500),
                                                        (e.verifyimg = t.verifyimg))
                                        })
                                    : 'tg' === this.type
                                    ? this.$axios
                                        .$post(
                                            '/ajax?ugi=bookingorder&action=createGroupTicketOrder',
                                            {
                                                usertype: 'tg',
                                                eventssessionid: this.$route.params.eventssessionid,
                                                bookingdata: JSON.stringify(this.userList),
                                            }
                                        )
                                        .then(function (t) {
                                            ;(e.loading = !1),
                                                'A00006' === t.code
                                                    ? e.$router.push({
                                                        name: 'type-success-orderid',
                                                        params: {
                                                            type: e.type,
                                                            orderid: t.ticketorder.orderid,
                                                        },
                                                    })
                                                    : 'A00013' === t.code &&
                                                    ((e.captchaType = t.captcha_type),
                                                        setTimeout(function () {
                                                            e.useVerify()
                                                        }, 500),
                                                        (e.verifyimg = t.verifyimg))
                                        })
                                    : 'ta' === this.type &&
                                    this.$axios
                                        .$post(
                                            '/ajax?ugi=bookingorder&action=createGroupTicketOrder',
                                            {
                                                usertype: 'ta',
                                                eventssessionid: this.$route.params.eventssessionid,
                                                bookingdata: JSON.stringify(this.userList),
                                                tourguideid: this.guides.guideid,
                                            }
                                        )
                                        .then(function (t) {
                                            ;(e.loading = !1),
                                                'A00006' === t.code
                                                    ? e.$router.replace({
                                                        name: 'type-success-orderid',
                                                        params: {
                                                            type: e.type,
                                                            orderid: t.ticketorder.orderid,
                                                        },
                                                    })
                                                    : 'A00013' === t.code &&
                                                    ((e.captchaType = t.captcha_type),
                                                        setTimeout(function () {
                                                            e.useVerify()
                                                        }, 500),
                                                        (e.verifyimg = t.verifyimg))
                                        })
                        },
                    },
                    head: function () {
                        var title = this.$t('JNT.createOrder.pageTitle')
                        return {
                            titleTemplate: function (e) {
                                return ''.concat(e, ' - ').concat(title)
                            },
                        }
                    },
                },
                f = h,
                _ = (r(944), r(43)),
                component = Object(_.a)(
                    f,
                    function () {
                        var e = this,
                            t = e._self._c
                        return t(
                            'div',
                            [
                                t('div', { staticClass: 'bg inner' }, [
                                    e._m(0),
                                    e._v(' '),
                                    t('div', { staticClass: 'content inner' }, [
                                        t('div', { staticClass: 'steps-new-container' }, [
                                            t('ul', { staticClass: 'steps' }, [
                                                t('li', { staticClass: 'active' }, [
                                                    t('span', [
                                                        e._v('1-' + e._s(e.$t('JNT.session.tip1'))),
                                                    ]),
                                                    e._v(' '),
                                                    t('div', { staticClass: 'line' }),
                                                ]),
                                                e._v(' '),
                                                t('li', { staticClass: 'active' }, [
                                                    t('span', [
                                                        e._v('2-' + e._s(e.$t('JNT.session.tip2'))),
                                                    ]),
                                                    e._v(' '),
                                                    t('div', { staticClass: 'line' }),
                                                ]),
                                                e._v(' '),
                                                t('li', { class: { active: e.confirmOrder } }, [
                                                    t('span', [
                                                        e._v('3-' + e._s(e.$t('JNT.session.tip3'))),
                                                    ]),
                                                    e._v(' '),
                                                    t('div', { staticClass: 'line' }),
                                                ]),
                                                e._v(' '),
                                                t('li', [
                                                    t('span', [
                                                        e._v('4-' + e._s(e.$t('JNT.session.tip4'))),
                                                    ]),
                                                ]),
                                            ]),
                                        ]),
                                        e._v(' '),
                                        t(
                                            'div',
                                            { staticClass: 'editorder-p' },
                                            [
                                                e.confirmOrder
                                                    ? t(
                                                    'i18n',
                                                    {
                                                        staticClass: 'process-date',
                                                        attrs: {
                                                            tag: 'div',
                                                            path: 'JNT.createOrder.tips',
                                                        },
                                                    },
                                                    [
                                                        t(
                                                            'span',
                                                            {
                                                                staticStyle: { color: 'rgb(255, 127, 0)' },
                                                                attrs: { place: 'date' },
                                                            },
                                                            [
                                                                e._v(
                                                                    '\n            ' +
                                                                    e._s(this.$route.query.date) +
                                                                    '\n            ' +
                                                                    e._s(this.$route.query.begintime) +
                                                                    '-' +
                                                                    e._s(this.$route.query.endtime) +
                                                                    '\n          '
                                                                ),
                                                            ]
                                                        ),
                                                    ]
                                                    )
                                                    : t('div', { staticClass: 'process-date' }, [
                                                        e._v(
                                                            '\n          ' +
                                                            e._s(e.$t('JNT.createOrder.date')) +
                                                            '：\n          ' +
                                                            e._s(this.$route.query.date) +
                                                            '\n          ' +
                                                            e._s(this.$route.query.begintime) +
                                                            '-' +
                                                            e._s(this.$route.query.endtime) +
                                                            '\n        '
                                                        ),
                                                    ]),
                                                e._v(' '),
                                                t('div', { staticClass: 'maintitle' }, [
                                                    t('span', [
                                                        e._v(e._s(e.$t('JNT.createOrder.people'))),
                                                    ]),
                                                    e._v(' '),
                                                    e.guides && e.confirmOrder
                                                        ? t('div', { staticStyle: { color: '#666' } }, [
                                                            e._v(
                                                                '\n            ' +
                                                                e._s(e.$t('JNT.tg_name')) +
                                                                '：' +
                                                                e._s(e.guides.realname) +
                                                                '\n          '
                                                            ),
                                                        ])
                                                        : e._e(),
                                                ]),
                                                e._v(' '),
                                                t('div', { staticClass: 'contact' }, [
                                                    e.confirmOrder
                                                        ? e._e()
                                                        : t('div', { staticClass: 'contact-menu' }, [
                                                            t(
                                                                'div',
                                                                { staticClass: 'importuserbox' },
                                                                [
                                                                    'user' !== e.type || e.isDev
                                                                        ? [
                                                                            t(
                                                                                'el-upload',
                                                                                {
                                                                                    attrs: {
                                                                                        'before-upload': e.handleUpload,
                                                                                        accept: '.txt',
                                                                                        action: 'default',
                                                                                    },
                                                                                },
                                                                                [
                                                                                    t(
                                                                                        'el-button',
                                                                                        { attrs: { size: 'small' } },
                                                                                        [
                                                                                            t('i', {
                                                                                                staticClass:
                                                                                                    'el-icon-folder',
                                                                                            }),
                                                                                            e._v(
                                                                                                '\n                    ' +
                                                                                                e._s(
                                                                                                    e.$t(
                                                                                                        'JNT.createOrder.import'
                                                                                                    )
                                                                                                ) +
                                                                                                '\n                  '
                                                                                            ),
                                                                                        ]
                                                                                    ),
                                                                                ],
                                                                                1
                                                                            ),
                                                                            e._v(' '),
                                                                            t(
                                                                                'el-button',
                                                                                {
                                                                                    staticStyle: {
                                                                                        'margin-left': '8px',
                                                                                    },
                                                                                    attrs: {
                                                                                        size: 'small',
                                                                                        type: 'text',
                                                                                    },
                                                                                    on: { click: e.downloadtxt },
                                                                                },
                                                                                [
                                                                                    e._v(
                                                                                        e._s(
                                                                                            e.$t(
                                                                                                'JNT.createOrder.downLoadtxt'
                                                                                            )
                                                                                        )
                                                                                    ),
                                                                                ]
                                                                            ),
                                                                        ]
                                                                        : e._e(),
                                                                ],
                                                                2
                                                            ),
                                                            e._v(' '),
                                                            t('div', { staticClass: 'btns' }, [
                                                                e.guides
                                                                    ? t(
                                                                    'div',
                                                                    {
                                                                        staticStyle: {
                                                                            'margin-right': '10px',
                                                                            color: '#666',
                                                                        },
                                                                    },
                                                                    [
                                                                        e._v(
                                                                            '\n                ' +
                                                                            e._s(e.$t('JNT.tg_name')) +
                                                                            '：' +
                                                                            e._s(e.guides.realname) +
                                                                            '\n              '
                                                                        ),
                                                                    ]
                                                                    )
                                                                    : e._e(),
                                                                e._v(' '),
                                                                'ta' === e.type
                                                                    ? t(
                                                                    'button',
                                                                    {
                                                                        staticClass: 'adduserbtn',
                                                                        staticStyle: {
                                                                            'margin-right': '16px',
                                                                        },
                                                                        on: { click: e.addGuides },
                                                                    },
                                                                    [
                                                                        t('i', {
                                                                            staticClass: 'iconfont iconjia',
                                                                        }),
                                                                        e._v(' '),
                                                                        t('span', [
                                                                            e._v(
                                                                                e._s(
                                                                                    e.$t('JNT.createOrder.addGuide')
                                                                                )
                                                                            ),
                                                                        ]),
                                                                    ]
                                                                    )
                                                                    : e._e(),
                                                                e._v(' '),
                                                                t(
                                                                    'button',
                                                                    {
                                                                        staticClass: 'adduserbtn',
                                                                        on: { click: e.addUser },
                                                                    },
                                                                    [
                                                                        t('i', {
                                                                            staticClass: 'iconfont iconjia',
                                                                        }),
                                                                        e._v(' '),
                                                                        t('span', [
                                                                            e._v(
                                                                                e._s(
                                                                                    e.$t('JNT.createOrder.addPeople')
                                                                                )
                                                                            ),
                                                                        ]),
                                                                    ]
                                                                ),
                                                            ]),
                                                        ]),
                                                    e._v(' '),
                                                    t(
                                                        'div',
                                                        {
                                                            staticClass: 'contact-form',
                                                            staticStyle: { 'margin-top': '15px' },
                                                        },
                                                        [
                                                            t(
                                                                'table',
                                                                { staticClass: 'common-table' },
                                                                [
                                                                    t(
                                                                        'thead',
                                                                        { staticClass: 'common-table-thead' },
                                                                        [
                                                                            t('tr', [
                                                                                t('th', [
                                                                                    e._v(
                                                                                        e._s(
                                                                                            e.$t(
                                                                                                'JNT.createOrder.orderNumber'
                                                                                            )
                                                                                        )
                                                                                    ),
                                                                                ]),
                                                                                e._v(' '),
                                                                                t('th', [
                                                                                    e._v(
                                                                                        e._s(
                                                                                            e.$t('JNT.createOrder.username')
                                                                                        )
                                                                                    ),
                                                                                ]),
                                                                                e._v(' '),
                                                                                t('th', [
                                                                                    e._v(
                                                                                        e._s(
                                                                                            e.$t('JNT.createOrder.doctype')
                                                                                        )
                                                                                    ),
                                                                                ]),
                                                                                e._v(' '),
                                                                                t('th', [
                                                                                    e._v(
                                                                                        e._s(e.$t('JNT.createOrder.idnum'))
                                                                                    ),
                                                                                ]),
                                                                                e._v(' '),
                                                                                e.confirmOrder
                                                                                    ? e._e()
                                                                                    : t(
                                                                                    'th',
                                                                                    {
                                                                                        staticStyle: { width: '120px' },
                                                                                    },
                                                                                    [
                                                                                        e._v(
                                                                                            '\n                    ' +
                                                                                            e._s(
                                                                                                e.$t(
                                                                                                    'JNT.createOrder.handle'
                                                                                                )
                                                                                            ) +
                                                                                            '\n                  '
                                                                                        ),
                                                                                    ]
                                                                                    ),
                                                                            ]),
                                                                        ]
                                                                    ),
                                                                    e._v(' '),
                                                                    t(
                                                                        'transition-group',
                                                                        {
                                                                            staticClass: 'common-table-tbody',
                                                                            attrs: {
                                                                                tag: 'tbody',
                                                                                name: 'slide-fade',
                                                                            },
                                                                        },
                                                                        e._l(e.userList, function (r, n) {
                                                                            return t(
                                                                                'tr',
                                                                                {
                                                                                    key: r.id,
                                                                                    class: {
                                                                                        self: r.isSelf,
                                                                                        invalid: r.invalid,
                                                                                    },
                                                                                },
                                                                                [
                                                                                    t('td', [e._v(e._s(n + 1))]),
                                                                                    e._v(' '),
                                                                                    t('td', [
                                                                                        e._v(
                                                                                            '\n                    ' +
                                                                                            e._s(r.realname)
                                                                                        ),
                                                                                        t('br'),
                                                                                        r.invalid && r.errmsg_realname
                                                                                            ? t(
                                                                                            'span',
                                                                                            {
                                                                                                staticStyle: {
                                                                                                    color: 'red',
                                                                                                },
                                                                                            },
                                                                                            [
                                                                                                e._v(
                                                                                                    e._s(r.errmsg_realname)
                                                                                                ),
                                                                                            ]
                                                                                            )
                                                                                            : e._e(),
                                                                                    ]),
                                                                                    e._v(' '),
                                                                                    t('td', [
                                                                                        e._v(
                                                                                            '\n                    ' +
                                                                                            e._s(
                                                                                                (
                                                                                                    e.doctypeOptions.find(
                                                                                                        function (option) {
                                                                                                            return (
                                                                                                                r.doctype ===
                                                                                                                option.value
                                                                                                            )
                                                                                                        }
                                                                                                    ) || { label: '' }
                                                                                                ).label
                                                                                            )
                                                                                        ),
                                                                                        t('br'),
                                                                                        r.invalid && r.errmsg_doctype
                                                                                            ? t(
                                                                                            'span',
                                                                                            {
                                                                                                staticStyle: {
                                                                                                    color: 'red',
                                                                                                },
                                                                                            },
                                                                                            [e._v(e._s(r.errmsg_doctype))]
                                                                                            )
                                                                                            : e._e(),
                                                                                    ]),
                                                                                    e._v(' '),
                                                                                    t('td', [
                                                                                        t(
                                                                                            'div',
                                                                                            { staticClass: 'td-idnum-inner' },
                                                                                            [
                                                                                                t(
                                                                                                    'div',
                                                                                                    { staticClass: 'idnumwrap' },
                                                                                                    [
                                                                                                        e._v(
                                                                                                            '\n                        ' +
                                                                                                            e._s(r.idnum) +
                                                                                                            '\n                      '
                                                                                                        ),
                                                                                                    ]
                                                                                                ),
                                                                                            ]
                                                                                        ),
                                                                                        e._v(' '),
                                                                                        r.invalid && r.errmsg_idnum
                                                                                            ? t(
                                                                                            'span',
                                                                                            {
                                                                                                staticStyle: {
                                                                                                    color: 'red',
                                                                                                },
                                                                                            },
                                                                                            [e._v(e._s(r.errmsg_idnum))]
                                                                                            )
                                                                                            : e._e(),
                                                                                    ]),
                                                                                    e._v(' '),
                                                                                    e.confirmOrder
                                                                                        ? e._e()
                                                                                        : t(
                                                                                        'td',
                                                                                        [
                                                                                            t(
                                                                                                'el-button',
                                                                                                {
                                                                                                    attrs: {
                                                                                                        type: 'text',
                                                                                                        disabled: r.isSelf,
                                                                                                    },
                                                                                                    on: {
                                                                                                        click: function (t) {
                                                                                                            return e.editUser(r)
                                                                                                        },
                                                                                                    },
                                                                                                },
                                                                                                [
                                                                                                    e._v(
                                                                                                        e._s(
                                                                                                            e.$t(
                                                                                                                'JNT.createOrder.editBtnTxt'
                                                                                                            )
                                                                                                        )
                                                                                                    ),
                                                                                                ]
                                                                                            ),
                                                                                            t(
                                                                                                'el-button',
                                                                                                {
                                                                                                    attrs: {
                                                                                                        type: 'text',
                                                                                                        disabled: r.isSelf,
                                                                                                    },
                                                                                                    on: {
                                                                                                        click: function (t) {
                                                                                                            return e.removeUser(n)
                                                                                                        },
                                                                                                    },
                                                                                                },
                                                                                                [
                                                                                                    e._v(
                                                                                                        e._s(
                                                                                                            e.$t(
                                                                                                                'JNT.createOrder.deleteBtnTxt'
                                                                                                            )
                                                                                                        )
                                                                                                    ),
                                                                                                ]
                                                                                            ),
                                                                                        ],
                                                                                        1
                                                                                        ),
                                                                                ]
                                                                            )
                                                                        }),
                                                                        0
                                                                    ),
                                                                ],
                                                                1
                                                            ),
                                                            e._v(' '),
                                                            e.userList.length
                                                                ? e._e()
                                                                : t(
                                                                'div',
                                                                {
                                                                    staticStyle: {
                                                                        'text-align': 'center',
                                                                        padding: '20px 0',
                                                                        color: '#999',
                                                                    },
                                                                },
                                                                [
                                                                    e._v(
                                                                        '\n              ' +
                                                                        e._s(
                                                                            e.$t(
                                                                                'JNT.createOrder.addUserPlease'
                                                                            )
                                                                        ) +
                                                                        '\n            '
                                                                    ),
                                                                ]
                                                                ),
                                                        ]
                                                    ),
                                                ]),
                                                e._v(' '),
                                                e.confirmOrder
                                                    ? e._e()
                                                    : t(
                                                    'div',
                                                    { staticClass: 'tips-wrap' },
                                                    [
                                                        e.maxnums > 0 || e.minnums > 0
                                                            ? t('div', { staticClass: 'process-tips' }, [
                                                                t('i', {
                                                                    staticClass: 'iconfont icongantanhao',
                                                                }),
                                                                t(
                                                                    'span',
                                                                    [
                                                                        e._v(
                                                                            e._s(
                                                                                e.$t(
                                                                                    'JNT.createOrder.limitTips[0]'
                                                                                )
                                                                            )
                                                                        ),
                                                                        e.minnums > 0
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t(
                                                                                            'JNT.createOrder.limitTips[1]',
                                                                                            { minnums: e.minnums }
                                                                                        )
                                                                                    ) + '，'
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e.maxnums > 0
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t(
                                                                                            'JNT.createOrder.limitTips[2]',
                                                                                            { maxnums: e.maxnums }
                                                                                        )
                                                                                    )
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                    ],
                                                                    2
                                                                ),
                                                            ])
                                                            : e._e(),
                                                        e._v(' '),
                                                        t(
                                                            'el-button',
                                                            {
                                                                staticStyle: { 'margin-right': '20px' },
                                                                attrs: {
                                                                    type: 'text',
                                                                    disabled: !e.userList.length,
                                                                    icon: 'el-icon-delete',
                                                                },
                                                                on: { click: e.delAll },
                                                            },
                                                            [
                                                                e._v(
                                                                    e._s(
                                                                        e.$t(
                                                                            'JNT.createOrder.delAll.handleButton'
                                                                        )
                                                                    )
                                                                ),
                                                            ]
                                                        ),
                                                    ],
                                                    1
                                                    ),
                                                e._v(' '),
                                                t(
                                                    'div',
                                                    { staticClass: 'editeorder-btn' },
                                                    [
                                                        e.confirmOrder
                                                            ? e._e()
                                                            : t(
                                                            'el-button',
                                                            {
                                                                attrs: {
                                                                    type: 'primary',
                                                                    disabled:
                                                                        !e.userList.length || e.showJkbDialog,
                                                                },
                                                                on: { click: e.handleSubmit },
                                                            },
                                                            [
                                                                e._v(
                                                                    '\n            ' +
                                                                    e._s(
                                                                        e.$t('JNT.createOrder.confirmBtntxt')
                                                                    ) +
                                                                    '\n          '
                                                                ),
                                                            ]
                                                            ),
                                                        e._v(' '),
                                                        e.confirmOrder
                                                            ? [
                                                                t(
                                                                    'el-button',
                                                                    {
                                                                        attrs: { type: 'info' },
                                                                        on: {
                                                                            click: function (t) {
                                                                                e.confirmOrder = !1
                                                                            },
                                                                        },
                                                                    },
                                                                    [
                                                                        e._v(
                                                                            '\n              ' +
                                                                            e._s(
                                                                                e.$t('JNT.createOrder.backBtntxt')
                                                                            ) +
                                                                            '\n            '
                                                                        ),
                                                                    ]
                                                                ),
                                                                e._v(' '),
                                                                t(
                                                                    'el-button',
                                                                    {
                                                                        attrs: {
                                                                            type: 'primary',
                                                                            loading: e.loading,
                                                                        },
                                                                        on: {
                                                                            click: function (t) {
                                                                                return e.$util.debounce(
                                                                                    e.handleConfirmOrder,
                                                                                    500,
                                                                                    !0
                                                                                )
                                                                            },
                                                                        },
                                                                    },
                                                                    [
                                                                        e._v(
                                                                            '\n              ' +
                                                                            e._s(
                                                                                e.$t('JNT.createOrder.submitBtntxt')
                                                                            ) +
                                                                            '\n            '
                                                                        ),
                                                                    ]
                                                                ),
                                                            ]
                                                            : e._e(),
                                                    ],
                                                    2
                                                ),
                                            ],
                                            1
                                        ),
                                    ]),
                                ]),
                                e._v(' '),
                                t(
                                    'el-dialog',
                                    {
                                        attrs: {
                                            'custom-class': 'dialog-modal',
                                            visible: e.showGuidesDialog,
                                        },
                                        on: {
                                            'update:visible': function (t) {
                                                e.showGuidesDialog = t
                                            },
                                        },
                                    },
                                    [
                                        t('div', { attrs: { slot: 'title' }, slot: 'title' }, [
                                            t('h5', { staticClass: 'header' }, [
                                                e._v(e._s(e.$t('JNT.createOrder.addGuidePopupTitle'))),
                                            ]),
                                        ]),
                                        e._v(' '),
                                        2 !== e.userInfo.joinstatus
                                            ? t(
                                            'el-table',
                                            {
                                                staticStyle: { width: '100%' },
                                                attrs: {
                                                    data: e.tglist,
                                                    height: '400px',
                                                    stripe: '',
                                                    'header-cell-style': {
                                                        backgroundColor: '#e9e9e9',
                                                    },
                                                },
                                            },
                                            [
                                                t('el-table-column', {
                                                    attrs: {
                                                        prop: 'realname',
                                                        label: e.$t('JNT.createOrder.guideName'),
                                                    },
                                                }),
                                                e._v(' '),
                                                t('el-table-column', {
                                                    attrs: {
                                                        label: e.$t('JNT.createOrder.guideStatus[0]'),
                                                    },
                                                    scopedSlots: e._u(
                                                        [
                                                            {
                                                                key: 'default',
                                                                fn: function (r) {
                                                                    return [
                                                                        0 === r.row.status
                                                                            ? [
                                                                                t(
                                                                                    'span',
                                                                                    {
                                                                                        staticStyle: {
                                                                                            color: '#e6a23c',
                                                                                        },
                                                                                    },
                                                                                    [
                                                                                        e._v(
                                                                                            '\n              ' +
                                                                                            e._s(
                                                                                                e.$t(
                                                                                                    'JNT.createOrder.guideStatus[1]'
                                                                                                )
                                                                                            ) +
                                                                                            '\n            '
                                                                                        ),
                                                                                    ]
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e._v(' '),
                                                                        1 === r.row.status
                                                                            ? [
                                                                                t(
                                                                                    'span',
                                                                                    {
                                                                                        staticStyle: {
                                                                                            color: '#67c23a',
                                                                                        },
                                                                                    },
                                                                                    [
                                                                                        e._v(
                                                                                            '\n              ' +
                                                                                            e._s(
                                                                                                e.$t(
                                                                                                    'JNT.createOrder.guideStatus[2]'
                                                                                                )
                                                                                            ) +
                                                                                            '\n            '
                                                                                        ),
                                                                                    ]
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e._v(' '),
                                                                        2 === r.row.status
                                                                            ? [
                                                                                t(
                                                                                    'span',
                                                                                    {
                                                                                        staticStyle: {
                                                                                            color: '#e6a23c',
                                                                                        },
                                                                                    },
                                                                                    [
                                                                                        e._v(
                                                                                            '\n              ' +
                                                                                            e._s(
                                                                                                e.$t(
                                                                                                    'JNT.createOrder.guideStatus[3]'
                                                                                                )
                                                                                            ) +
                                                                                            '\n            '
                                                                                        ),
                                                                                    ]
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e._v(' '),
                                                                        8 === r.row.status
                                                                            ? [
                                                                                t(
                                                                                    'span',
                                                                                    {
                                                                                        staticStyle: {
                                                                                            color: '#f56c6c',
                                                                                        },
                                                                                    },
                                                                                    [
                                                                                        e._v(
                                                                                            '\n              ' +
                                                                                            e._s(
                                                                                                e.$t(
                                                                                                    'JNT.createOrder.guideStatus[4]'
                                                                                                )
                                                                                            ) +
                                                                                            '\n            '
                                                                                        ),
                                                                                    ]
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                    ]
                                                                },
                                                            },
                                                        ],
                                                        null,
                                                        !1,
                                                        3094039694
                                                    ),
                                                }),
                                                e._v(' '),
                                                t('el-table-column', {
                                                    attrs: {
                                                        label: e.$t('JNT.createOrder.operate[0]'),
                                                        width: '100',
                                                    },
                                                    scopedSlots: e._u(
                                                        [
                                                            {
                                                                key: 'default',
                                                                fn: function (r) {
                                                                    return [
                                                                        r.row.isConflict
                                                                            ? t(
                                                                            'el-button',
                                                                            {
                                                                                attrs: {
                                                                                    disabled: '',
                                                                                    type: 'text',
                                                                                    size: 'small',
                                                                                },
                                                                            },
                                                                            [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t(
                                                                                            'JNT.createOrder.operate[1]'
                                                                                        )
                                                                                    )
                                                                                ),
                                                                            ]
                                                                            )
                                                                            : t(
                                                                            'el-button',
                                                                            {
                                                                                attrs: {
                                                                                    disabled: 8 === r.row.status,
                                                                                    type: 'text',
                                                                                    size: 'small',
                                                                                },
                                                                                on: {
                                                                                    click: function (t) {
                                                                                        return e.handleConfirmAddGuides(
                                                                                            r.row
                                                                                        )
                                                                                    },
                                                                                },
                                                                            },
                                                                            [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t(
                                                                                            'JNT.createOrder.operate[2]'
                                                                                        )
                                                                                    )
                                                                                ),
                                                                            ]
                                                                            ),
                                                                    ]
                                                                },
                                                            },
                                                        ],
                                                        null,
                                                        !1,
                                                        88431135
                                                    ),
                                                }),
                                            ],
                                            1
                                            )
                                            : e._e(),
                                    ],
                                    1
                                ),
                                e._v(' '),
                                t(
                                    'el-dialog',
                                    {
                                        attrs: {
                                            'custom-class': 'dialog-modal ',
                                            visible: e.dialogShow,
                                        },
                                        on: {
                                            'update:visible': function (t) {
                                                e.dialogShow = t
                                            },
                                        },
                                    },
                                    [
                                        t('div', { attrs: { slot: 'title' }, slot: 'title' }, [
                                            t('h5', { staticClass: 'header' }, [
                                                e._v(
                                                    '\n        ' +
                                                    e._s(
                                                        e.selectedUser
                                                            ? e.$t('JNT.createOrder.addUserPopupTitle')
                                                            : e.$t('JNT.createOrder.editUserPopupTitle')
                                                    ) +
                                                    '\n      '
                                                ),
                                            ]),
                                        ]),
                                        e._v(' '),
                                        t(
                                            'div',
                                            { staticClass: 'adduser-dialog-body' },
                                            [
                                                t(
                                                    'el-form',
                                                    {
                                                        ref: 'signForm',
                                                        staticClass: 'form-s',
                                                        staticStyle: { width: '460px', margin: '0' },
                                                        attrs: {
                                                            model: e.signForm,
                                                            rules: e.rules,
                                                            'status-icon': '',
                                                            'label-width': '110px',
                                                        },
                                                    },
                                                    [
                                                        t(
                                                            'el-form-item',
                                                            {
                                                                attrs: {
                                                                    label: e.$t(
                                                                        'JNT.createOrder.addUserForm.realname.label'
                                                                    ),
                                                                    prop: 'realname',
                                                                },
                                                            },
                                                            [
                                                                t('el-input', {
                                                                    attrs: {
                                                                        placeholder: e.$t(
                                                                            'JNT.createOrder.addUserForm.realname.placeholder'
                                                                        ),
                                                                        name: 'realname',
                                                                    },
                                                                    on: {
                                                                        change: function (t) {
                                                                            e.signForm.realname = t
                                                                                .trim()
                                                                                .replace(/\s+/g, ' ')
                                                                        },
                                                                    },
                                                                    nativeOn: {
                                                                        paste: function (t) {
                                                                            return e.onpaste.apply(null, arguments)
                                                                        },
                                                                    },
                                                                    model: {
                                                                        value: e.signForm.realname,
                                                                        callback: function (t) {
                                                                            e.$set(e.signForm, 'realname', t)
                                                                        },
                                                                        expression: 'signForm.realname',
                                                                    },
                                                                }),
                                                                e._v(' '),
                                                                t('div', { staticClass: 'tips' }, [
                                                                    e._v(
                                                                        '\n            ' +
                                                                        e._s(
                                                                            e.$t(
                                                                                'JNT.createOrder.addUserForm.realname.tips'
                                                                            )
                                                                        ) +
                                                                        '\n          '
                                                                    ),
                                                                ]),
                                                            ],
                                                            1
                                                        ),
                                                        e._v(' '),
                                                        t(
                                                            'el-form-item',
                                                            {
                                                                attrs: {
                                                                    label: e.$t(
                                                                        'JNT.createOrder.addUserForm.doctype.label'
                                                                    ),
                                                                    prop: 'doctype',
                                                                },
                                                            },
                                                            [
                                                                t(
                                                                    'el-select',
                                                                    {
                                                                        attrs: {
                                                                            placeholder: e.$t(
                                                                                'JNT.createOrder.addUserForm.doctype.placeholder'
                                                                            ),
                                                                            name: 'doctype',
                                                                        },
                                                                        on: {
                                                                            change: function (t) {
                                                                                e.signForm.idnum &&
                                                                                e.$refs.signForm.validateField(
                                                                                    'idnum'
                                                                                ),
                                                                                e.signForm.realname &&
                                                                                e.$refs.signForm.validateField(
                                                                                    'realname'
                                                                                )
                                                                            },
                                                                        },
                                                                        model: {
                                                                            value: e.signForm.doctype,
                                                                            callback: function (t) {
                                                                                e.$set(e.signForm, 'doctype', t)
                                                                            },
                                                                            expression: 'signForm.doctype',
                                                                        },
                                                                    },
                                                                    e._l(e.doctypeOptions, function (e, r) {
                                                                        return t('el-option', {
                                                                            key: r,
                                                                            attrs: { label: e.label, value: e.value },
                                                                        })
                                                                    }),
                                                                    1
                                                                ),
                                                            ],
                                                            1
                                                        ),
                                                        e._v(' '),
                                                        t(
                                                            'el-form-item',
                                                            {
                                                                attrs: {
                                                                    label: e.$t(
                                                                        'JNT.createOrder.addUserForm.idnum.label'
                                                                    ),
                                                                    prop: 'idnum',
                                                                },
                                                            },
                                                            [
                                                                t('el-input', {
                                                                    attrs: {
                                                                        placeholder: e.$t(
                                                                            'JNT.createOrder.addUserForm.idnum.placeholder'
                                                                        ),
                                                                        name: 'idnum',
                                                                    },
                                                                    model: {
                                                                        value: e.signForm.idnum,
                                                                        callback: function (t) {
                                                                            e.$set(e.signForm, 'idnum', t)
                                                                        },
                                                                        expression: 'signForm.idnum',
                                                                    },
                                                                }),
                                                                e._v(' '),
                                                                t(
                                                                    'div',
                                                                    { staticClass: 'tips' },
                                                                    [
                                                                        'IDCARD' === e.signForm.doctype
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t('JNT.user.registry.example')
                                                                                    ) + '：440102198001021230'
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e._v(' '),
                                                                        'PASSPORT' === e.signForm.doctype
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t('JNT.user.registry.example')
                                                                                    ) +
                                                                                    '：141234567, G12345678,\n              P1234567'
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e._v(' '),
                                                                        'SOLDIER' === e.signForm.doctype
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t('JNT.user.registry.example')
                                                                                    ) +
                                                                                    '：军字第2001988号,\n              士字第P011816X号'
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e._v(' '),
                                                                        'HMCARD' === e.signForm.doctype
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t('JNT.user.registry.example')
                                                                                    ) + '：H1234567890'
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                        e._v(' '),
                                                                        'TCARD' === e.signForm.doctype
                                                                            ? [
                                                                                e._v(
                                                                                    e._s(
                                                                                        e.$t('JNT.user.registry.example')
                                                                                    ) +
                                                                                    '：12345678\n              ' +
                                                                                    e._s(
                                                                                        e.$t('JNT.user.registry.or')
                                                                                    ) +
                                                                                    ' 123456789B'
                                                                                ),
                                                                            ]
                                                                            : e._e(),
                                                                    ],
                                                                    2
                                                                ),
                                                            ],
                                                            1
                                                        ),
                                                    ],
                                                    1
                                                ),
                                            ],
                                            1
                                        ),
                                        e._v(' '),
                                        t(
                                            'div',
                                            { staticClass: 'btn-group' },
                                            [
                                                t(
                                                    'el-button',
                                                    {
                                                        attrs: { type: 'primary' },
                                                        on: { click: e.handleAdd },
                                                    },
                                                    [e._v('确定')]
                                                ),
                                                e._v(' '),
                                                t(
                                                    'el-button',
                                                    {
                                                        attrs: { type: 'info', plain: '' },
                                                        on: {
                                                            click: function (t) {
                                                                e.dialogShow = !1
                                                            },
                                                        },
                                                    },
                                                    [e._v('取消')]
                                                ),
                                            ],
                                            1
                                        ),
                                    ]
                                ),
                                e._v(' '),
                                t('Verify', {
                                    ref: 'verify',
                                    attrs: {
                                        mode: 'pop',
                                        captchaType: e.captchaType,
                                        'img-size': { width: '330px', height: '155px' },
                                    },
                                    on: { success: e.success },
                                }),
                                e._v(' '),
                                t(
                                    'el-dialog',
                                    {
                                        attrs: {
                                            'custom-class': 'dialog-modal',
                                            visible: e.showJkbDialog,
                                        },
                                        on: {
                                            'update:visible': function (t) {
                                                e.showJkbDialog = t
                                            },
                                        },
                                    },
                                    [
                                        t('div', { attrs: { slot: 'title' }, slot: 'title' }, [
                                            t('h5', { staticClass: 'header' }, [e._v('提示')]),
                                        ]),
                                        e._v(' '),
                                        t('div', { staticClass: 'jkb-dialog-body' }, [
                                            t('div', { staticClass: 'p1' }, [
                                                e._v('根据北京市疫情防护要求，需先完成北京健康宝注册'),
                                            ]),
                                            e._v(' '),
                                            t('img', { attrs: { src: r(910), alt: '北京健康宝' } }),
                                            e._v(' '),
                                            t('div', { staticClass: 'p2' }, [
                                                e._v(
                                                    '\n        可扫描二维码或通过微信搜索【北京健康宝】小程序，进行注册\n      '
                                                ),
                                            ]),
                                        ]),
                                        e._v(' '),
                                        t(
                                            'div',
                                            { staticClass: 'btn-group' },
                                            [
                                                t(
                                                    'el-button',
                                                    {
                                                        staticStyle: { width: '150px' },
                                                        attrs: { type: 'primary' },
                                                        on: {
                                                            click: function (t) {
                                                                e.showJkbDialog = !1
                                                            },
                                                        },
                                                    },
                                                    [e._v('知道了')]
                                                ),
                                            ],
                                            1
                                        ),
                                    ]
                                ),
                            ],
                            1
                        )
                    },
                    [
                        function () {
                            var e = this._self._c
                            return e('div', { staticClass: 'banner inner' }, [
                                e('div', { staticClass: 'banner-img' }),
                            ])
                        },
                    ],
                    !1,
                    null,
                    'e59228b0',
                    null
                )
            t.default = component.exports
        },
    },
])
